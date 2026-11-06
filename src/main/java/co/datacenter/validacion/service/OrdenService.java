package co.datacenter.validacion.service;

import co.datacenter.validacion.client.CrearPagoRemoto;
import co.datacenter.validacion.client.RecaudoClient;
import co.datacenter.validacion.exception.NegocioException;
import co.datacenter.validacion.mapper.OrdenMapper;
import co.datacenter.validacion.persistence.EstadoOrden;
import co.datacenter.validacion.persistence.OrdenEntity;
import co.datacenter.validacion.persistence.OrdenRepository;
import co.datacenter.validacion.request.CrearOrdenRequest;
import co.datacenter.validacion.request.EventoOrdenRequest;
import co.datacenter.validacion.response.PagoResponse;
import co.datacenter.validacion.util.HashCuerpo;
import co.datacenter.validacion.util.Mascara;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenes;
    private final OrdenMapper mapper;
    private final IdempotenciaService idempotencia;
    private final RecaudoClient recaudo;
    private final ObjectMapper json;

    public ResultadoOrden crear(CrearOrdenRequest request) {
        String hash = HashCuerpo.de(request);
        String clave = IdempotenciaService.claveOrden(request.referencia().trim());
        Optional<OrdenEntity> existente = ordenes.findByReferencia(request.referencia().trim());
        if (existente.isPresent()) {
            return repetir(existente.get(), hash, clave);
        }
        Optional<String> candado = idempotencia.leer(clave);
        if (candado.filter(IdempotenciaService.EN_PROCESO::equals).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Link en proceso");
        }
        if (!idempotencia.tomar(clave)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Link en proceso");
        }
        OrdenEntity orden = mapper.nueva(request);
        try {
            ordenes.saveAndFlush(orden);
        } catch (DataIntegrityViolationException ex) {
            idempotencia.soltar(clave);
            return ordenes.findByReferencia(orden.getReferencia())
                    .map(guardada -> repetir(guardada, hash, clave))
                    .orElseThrow(() -> ex);
        }
        try {
            PagoResponse respuesta = recaudo.crearPago(remoto(orden, 1));
            guardarRespuesta(orden, respuesta);
            idempotencia.guardarRespuesta(clave, json.writeValueAsString(respuesta));
            log.info("Orden creada referencia={} documento={} idPago={} estadoPago={}",
                    orden.getReferencia(), Mascara.documento(orden.getDocumentoCliente()),
                    respuesta.idPago(), respuesta.estado());
            HttpStatus status = respuesta.code() == 202 ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
            return new ResultadoOrden(status, respuesta);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 503) {
                ordenes.delete(orden);
                idempotencia.soltar(clave);
                throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "Pasarela no disponible");
            }
            throw ex;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo guardar la respuesta de la orden", ex);
        }
    }

    public ResultadoOrden abrirIntento(String referencia) {
        OrdenEntity orden = ordenes.findByReferencia(referencia)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La orden no existe"));
        if (orden.getEstado() == EstadoOrden.PAGADA) {
            throw new NegocioException(HttpStatus.CONFLICT, "La orden ya está pagada");
        }
        PagoResponse vigente = recaudo.consultarPago(orden.getIdPago());
        if (vigente.estado() != null && (vigente.estado() == 3 || vigente.estado() == 4)) {
            throw new NegocioException(HttpStatus.CONFLICT, "El pago sigue en confirmación");
        }
        if (vigente.estado() == null || vigente.estado() != 2) {
            throw new NegocioException(HttpStatus.CONFLICT, "El intento anterior no está rechazado");
        }
        String clave = IdempotenciaService.claveIntento(referencia);
        if (!idempotencia.tomar(clave)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Link en proceso");
        }
        PagoResponse respuesta = recaudo.crearPago(remoto(orden, 0));
        guardarRespuesta(orden, respuesta);
        try {
            idempotencia.guardarRespuesta(clave, json.writeValueAsString(respuesta));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo guardar el intento", ex);
        }
        return new ResultadoOrden(HttpStatus.CREATED, respuesta);
    }

    /**
     * El worker de recaudo llama este método. La orden solo cambia si sigue abierta:
     * un aprobado posterior no se pisa con un rechazo tardío.
     */
    @Transactional
    public void aplicarEvento(EventoOrdenRequest evento) {
        OrdenEntity orden = ordenes.findByReferencia(evento.referencia())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La orden no existe"));
        EstadoOrden destino = mapear(evento.tipo());
        if (orden.getEstado() == EstadoOrden.PAGADA || orden.getEstado() == EstadoOrden.EXPIRADA) {
            log.info("Evento {} ignorado. La orden {} ya está {}", evento.tipo(), evento.referencia(), orden.getEstado());
            return;
        }
        if (destino != EstadoOrden.PAGADA
                && orden.getEstado() != EstadoOrden.EN_PROCESO
                && orden.getEstado() != EstadoOrden.POR_CONCILIAR) {
            log.info("Evento {} ignorado. La orden {} ya está {}", evento.tipo(), evento.referencia(), orden.getEstado());
            return;
        }
        EstadoOrden anterior = orden.getEstado();
        orden.setEstado(destino);
        orden.setIdPago(evento.idPago());
        orden.setVersion(orden.getVersion() + 1);
        orden.setActualizadoEn(Instant.now());
        log.info("Orden {} {} -> {} por {}", orden.getReferencia(), anterior, orden.getEstado(), evento.tipo());
    }

    @Transactional
    public void marcarPorConciliar(String referencia) {
        ordenes.findByReferencia(referencia).ifPresent(orden -> {
            if (orden.getEstado() == EstadoOrden.EN_PROCESO) {
                orden.setEstado(EstadoOrden.POR_CONCILIAR);
                orden.setActualizadoEn(Instant.now());
            }
        });
    }

    private ResultadoOrden repetir(OrdenEntity orden, String hash, String clave) {
        if (!orden.getRequestHash().equals(hash)) {
            throw new NegocioException(HttpStatus.CONFLICT, "La referencia ya pertenece a otra orden");
        }
        if (orden.getRespuestaJson() == null) {
            throw new NegocioException(HttpStatus.CONFLICT, "Link en proceso");
        }
        try {
            PagoResponse respuesta = json.readValue(orden.getRespuestaJson(), PagoResponse.class);
            idempotencia.guardarRespuesta(clave, orden.getRespuestaJson());
            return new ResultadoOrden(HttpStatus.OK, respuesta);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("La respuesta guardada no se puede leer", ex);
        }
    }

    private void guardarRespuesta(OrdenEntity orden, PagoResponse respuesta) {
        try {
            orden.setIdPago(respuesta.idPago());
            orden.setRespuestaJson(json.writeValueAsString(respuesta));
            orden.setActualizadoEn(Instant.now());
            orden.setVersion(orden.getVersion() + 1);
            ordenes.save(orden);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar PagoResponse", ex);
        }
    }

    private CrearPagoRemoto remoto(OrdenEntity orden, int numeroIntento) {
        return new CrearPagoRemoto(
                orden.getIdOrden(),
                orden.getReferencia(),
                numeroIntento,
                orden.getDocumentoCliente(),
                orden.getNombreCliente(),
                orden.getDescripcion(),
                orden.getValor(),
                orden.getMoneda());
    }

    private EstadoOrden mapear(String tipo) {
        return switch (tipo) {
            case "PAGO_APROBADO" -> EstadoOrden.PAGADA;
            case "PAGO_RECHAZADO" -> EstadoOrden.RECHAZADA;
            case "PAGO_EXPIRADO" -> EstadoOrden.EXPIRADA;
            default -> throw new NegocioException(HttpStatus.BAD_REQUEST, "Tipo de evento desconocido");
        };
    }

    public record ResultadoOrden(HttpStatus status, PagoResponse cuerpo) {
    }
}

package co.datacenter.validacion.mapper;

import co.datacenter.validacion.persistence.EstadoOrden;
import co.datacenter.validacion.persistence.OrdenEntity;
import co.datacenter.validacion.request.CrearOrdenRequest;
import co.datacenter.validacion.util.HashCuerpo;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrdenMapper {

    public OrdenEntity nueva(CrearOrdenRequest request) {
        Instant ahora = Instant.now();
        OrdenEntity orden = new OrdenEntity();
        orden.setIdOrden(UUID.randomUUID().toString());
        orden.setReferencia(request.referencia().trim());
        orden.setDocumentoCliente(request.documentoCliente().trim());
        orden.setNombreCliente(request.nombreCliente().trim());
        orden.setDescripcion(request.descripcionOrden().trim());
        orden.setValor(request.valorOrden());
        orden.setMoneda(request.moneda().trim().toUpperCase());
        orden.setEstado(EstadoOrden.EN_PROCESO);
        orden.setRequestHash(HashCuerpo.de(request));
        orden.setVersion(0);
        orden.setCreadoEn(ahora);
        orden.setActualizadoEn(ahora);
        return orden;
    }
}

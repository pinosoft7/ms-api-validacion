package co.datacenter.validacion.controller;

import co.datacenter.validacion.request.CrearOrdenRequest;
import co.datacenter.validacion.request.EventoOrdenRequest;
import co.datacenter.validacion.response.PagoResponse;
import co.datacenter.validacion.service.OrdenService;
import co.datacenter.validacion.service.OrdenService.ResultadoOrden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Orden")
public class OrdenController {

    private final OrdenService ordenes;

    @PostMapping("/ordenes")
    @Operation(summary = "Crea la orden y pide el primer link de pago")
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody CrearOrdenRequest request) {
        ResultadoOrden resultado = ordenes.crear(request);
        return ResponseEntity.status(resultado.status()).body(resultado.cuerpo());
    }

    @PostMapping("/ordenes/{referencia}/intentos")
    @Operation(summary = "Abre otro checkout de la misma orden cuando el intento anterior está rechazado")
    public ResponseEntity<PagoResponse> intento(@PathVariable String referencia) {
        ResultadoOrden resultado = ordenes.abrirIntento(referencia);
        return ResponseEntity.status(resultado.status()).body(resultado.cuerpo());
    }

    @PostMapping("/interno/eventos")
    @Operation(summary = "Aplica el evento que entrega el worker de recaudo")
    public ResponseEntity<Void> evento(@Valid @RequestBody EventoOrdenRequest evento) {
        ordenes.aplicarEvento(evento);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/interno/ordenes/{referencia}/por-conciliar")
    @Operation(summary = "Deja la orden en POR_CONCILIAR cuando se agota la ventana de 24 horas")
    public ResponseEntity<Void> porConciliar(@PathVariable String referencia) {
        ordenes.marcarPorConciliar(referencia);
        return ResponseEntity.ok().build();
    }
}

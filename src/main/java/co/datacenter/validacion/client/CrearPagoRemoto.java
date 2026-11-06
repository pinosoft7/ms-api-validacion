package co.datacenter.validacion.client;

import java.math.BigDecimal;

public record CrearPagoRemoto(
        String idOrden,
        String referencia,
        int numeroIntento,
        String documentoCliente,
        String nombreCliente,
        String descripcionOrden,
        BigDecimal valorOrden,
        String moneda
) {
}

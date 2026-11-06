package co.datacenter.validacion.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CrearOrdenRequest(
        @NotBlank String documentoCliente,
        @NotBlank @Size(max = 120) String nombreCliente,
        @NotBlank @Size(min = 10, max = 500) String descripcionOrden,
        @NotNull @DecimalMin("0.01") BigDecimal valorOrden,
        @NotBlank @Size(min = 3, max = 3) String moneda,
        @NotBlank @Size(max = 40) String referencia
) {
}

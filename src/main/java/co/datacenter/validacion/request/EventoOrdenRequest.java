package co.datacenter.validacion.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EventoOrdenRequest(
        @NotBlank String referencia,
        @NotBlank String tipo,
        @NotNull BigDecimal valor,
        @NotBlank String idPago
) {
}

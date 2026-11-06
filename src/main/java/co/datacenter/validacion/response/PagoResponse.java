package co.datacenter.validacion.response;

public record PagoResponse(
        int code,
        String error,
        String linkPasarela,
        Integer estado,
        String referencia,
        String idPago,
        Integer numeroIntento
) {
}

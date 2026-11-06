package co.datacenter.validacion.client;

import co.datacenter.validacion.response.PagoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RecaudoClient {

    private final RestClient recaudoRestClient;

    public PagoResponse crearPago(CrearPagoRemoto solicitud) {
        return recaudoRestClient.post()
                .uri("/pagos")
                .body(solicitud)
                .retrieve()
                .body(PagoResponse.class);
    }

    public PagoResponse consultarPago(String idPago) {
        return recaudoRestClient.get()
                .uri("/pagos/{idPago}", idPago)
                .retrieve()
                .body(PagoResponse.class);
    }
}

package co.datacenter.validacion.util;

import co.datacenter.validacion.request.CrearOrdenRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashCuerpo {

    private HashCuerpo() {
    }

    public static String de(CrearOrdenRequest request) {
        String canonico = String.join("|",
                request.documentoCliente().trim(),
                request.nombreCliente().trim(),
                request.descripcionOrden().trim(),
                request.valorOrden().toPlainString(),
                request.moneda().trim().toUpperCase(),
                request.referencia().trim());
        return sha256(canonico);
    }

    private static String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}

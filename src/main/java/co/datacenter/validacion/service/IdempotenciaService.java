package co.datacenter.validacion.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Candado corto. Si Redis no responde, la unicidad de la referencia en Oracle hace el corte.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class IdempotenciaService {

    static final String EN_PROCESO = "EN_PROCESO";
    private static final Duration CANDADO = Duration.ofSeconds(120);
    private static final Duration RESPUESTA = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public Optional<String> leer(String clave) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(clave));
        } catch (RuntimeException ex) {
            log.warn("Redis no respondió al leer {}. Sigue Oracle.", clave);
            return Optional.empty();
        }
    }

    public boolean tomar(String clave) {
        try {
            Boolean creado = redis.opsForValue().setIfAbsent(clave, EN_PROCESO, CANDADO);
            return Boolean.TRUE.equals(creado);
        } catch (RuntimeException ex) {
            log.warn("Redis no respondió al tomar {}. La orden sigue si la referencia es nueva.", clave);
            return true;
        }
    }

    public void guardarRespuesta(String clave, String json) {
        try {
            redis.opsForValue().set(clave, json, RESPUESTA);
        } catch (RuntimeException ex) {
            log.warn("No se pudo guardar la respuesta idempotente de {}.", clave);
        }
    }

    public void soltar(String clave) {
        try {
            redis.delete(clave);
        } catch (RuntimeException ex) {
            log.warn("No se pudo soltar el candado {}.", clave);
        }
    }

    public static String claveOrden(String referencia) {
        return "idempotency:" + referencia;
    }

    public static String claveIntento(String referencia) {
        return "idempotency:" + referencia + ":intento";
    }
}

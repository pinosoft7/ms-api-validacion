package co.datacenter.validacion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El canal debe presentar un token. Quién lo emite queda fuera de este servicio.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CanalFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.startsWith("/interno")
                || ruta.startsWith("/swagger-ui")
                || ruta.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String autorizacion = request.getHeader("Authorization");
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token del canal requerido");
            return;
        }
        chain.doFilter(request, response);
    }
}

package com.clubpadelcostabrava.server.auth;

import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token Bearer de cada petición protegida y prepara
 * la identidad y el rol para Spring Security.
 */
public class FiltroSesion extends OncePerRequestFilter {

    private final SesionService sesiones;
    private final CuentaRepository cuentas;

    /**
     * Recibe el almacén de sesiones y el acceso a cuentas.
     *
     * @param sesiones búsqueda e invalidación de tokens
     * @param cuentas  comprobación de que la cuenta sigue habilitada
     */
    public FiltroSesion(SesionService sesiones, CuentaRepository cuentas) {
        this.sesiones = sesiones;
        this.cuentas = cuentas;
    }

    /**
     * Comprueba que el token exista, no haya caducado y pertenezca
     * a una cuenta todavía habilitada. Si es válido, establece
     * la autenticación durante el resto de la petición.
     *
     * @param request  petición HTTP recibida
     * @param response respuesta HTTP
     * @param chain    cadena de filtros que continúa la petición
     * @throws ServletException si falla el procesamiento del filtro
     * @throws IOException      si falla la lectura o escritura HTTP
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");

        if (cabecera == null) {
            chain.doFilter(request, response);
            return;
        }

        String token = cabecera.startsWith("Bearer ")
                ? cabecera.substring("Bearer ".length())
                : null;

        SesionService.Sesion sesion = token == null
                ? null
                : sesiones.buscar(token).orElse(null);

        if (sesion == null) {
            responderSesionInvalida(response);
            return;
        }

        if (!cuentas.cuentaHabilitada(sesion.cuentaId(), sesion.rol())) {
            sesiones.invalidar(token);
            responderSesionInvalida(response);
            return;
        }
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                sesion.cuentaId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + sesion.rol()))
        );

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Omite el filtro de sesión en las rutas públicas.
     *
     * @param request petición HTTP recibida
     * @return true si la ruta no requiere comprobar un token previo
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getServletPath();

        return "/api/diagnostico".equals(ruta)
                || "/api/auth/login".equals(ruta)
                || "/api/auth/codigo".equals(ruta)
                || "/api/auth/contrasena".equals(ruta);
    }

    /**
     * Escribe el error JSON y el estado HTTP 401 de una sesión inválida.
     *
     * @param response respuesta HTTP que se enviará al cliente
     * @throws IOException si falla la escritura de la respuesta
     */
    private void responderSesionInvalida(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"codigo\":\"SESION_INVALIDA\",\"mensaje\":\"Sesión no válida o caducada.\"}"
        );
    }
}

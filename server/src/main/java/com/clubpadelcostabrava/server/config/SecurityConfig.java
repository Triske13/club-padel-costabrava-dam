package com.clubpadelcostabrava.server.config;

import com.clubpadelcostabrava.server.auth.FiltroSesion;
import com.clubpadelcostabrava.server.auth.SesionService;
import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Configura la autenticación por token, los permisos de las rutas
 * y el codificador de contraseñas del servidor.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configura la API sin sesión HTTP y coloca el filtro Bearer
     * antes de la comprobación de permisos.
     * Permite las rutas públicas, exige ADMIN en /api/admin/**
     * y USER en /api/mis-reservas/**.
     *
     * @param http     configuración de seguridad HTTP
     * @param sesiones almacén de tokens
     * @param cuentas  comprobación del estado de las cuentas
     * @return cadena de filtros de seguridad
     * @throws Exception si Spring Security no puede construir la cadena
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SesionService sesiones,
            CuentaRepository cuentas
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"codigo\":\"SESION_INVALIDA\",\"mensaje\":\"Debes iniciar sesión.\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"codigo\":\"ACCESO_DENEGADO\",\"mensaje\":\"No tienes permiso para esta operación.\"}"
                            );
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/diagnostico").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/codigo",
                                "/api/auth/contrasena").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/mis-reservas", "/api/mis-reservas/**")
                        .hasRole("USER")
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(
                new FiltroSesion(sesiones, cuentas),
                AuthorizationFilter.class
        );
        return http.build();
    }

    /**
     * Proporciona PBKDF2 para proteger y comprobar contraseñas y códigos.
     *
     * @return codificador de contraseñas compartido por la aplicación
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}

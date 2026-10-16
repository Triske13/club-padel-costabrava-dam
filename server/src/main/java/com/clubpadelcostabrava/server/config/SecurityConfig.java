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

/** Define la autenticación, los permisos por ruta y el codificador de contraseñas. */
@Configuration
public class SecurityConfig {

    /** Configura una API sin sesión HTTP y coloca el filtro de tokens antes de autorizar. */
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
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(
                new FiltroSesion(sesiones, cuentas),
                AuthorizationFilter.class
        );
        return http.build();
    }

    /** Expone PBKDF2 para crear hashes y verificar contraseñas en toda la aplicación. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}

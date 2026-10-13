package com.clubpadelcostabrava.server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminInicial implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminInicial(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cuenta WHERE rol = 'ADMIN'",
                Integer.class
        );

        if (cantidad != null && cantidad > 0) {
            return;
        }

        String correo = System.getenv("APP_ADMIN_EMAIL");
        String contrasena = System.getenv("APP_ADMIN_PASSWORD");

        if (correo == null || contrasena == null) {
            throw new IllegalStateException(
                    "Para crear el ADMIN inicial faltan APP_ADMIN_EMAIL o APP_ADMIN_PASSWORD"
            );
        }

        correo = correo.strip().toLowerCase(Locale.ROOT);

        if (correo.length() > 255 ||
                !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("El correo inicial del ADMIN no es válido");
        }

        int longitud = contrasena.codePointCount(0, contrasena.length());
        if (longitud < 10 || longitud > 64) {
            throw new IllegalStateException(
                    "La contraseña inicial del ADMIN debe tener entre 10 y 64 caracteres"
            );
        }

        jdbcTemplate.update(
                "INSERT INTO cuenta (correo, contrasena_hash, rol) VALUES (?, ?, 'ADMIN')",
                correo,
                passwordEncoder.encode(contrasena)
        );
    }
}
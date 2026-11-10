package com.clubpadelcostabrava.server.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Crea la primera cuenta ADMIN al arrancar el servidor,
 * únicamente si todavía no existe ninguna.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * Recibe el acceso a la base de datos y el codificador de contraseñas.
     *
     * @param jdbcTemplate    acceso JDBC a la base de datos
     * @param passwordEncoder protección de la contraseña inicial
     */
    public AdminInicial(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Comprueba si ya existe una cuenta ADMIN. Si no existe, lee
     * APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD, valida sus valores
     * y crea la cuenta inicial.
     *
     * @param args argumentos de arranque recibidos de Spring Boot
     * @throws IllegalStateException si falta o no es válida la configuración inicial
     */
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
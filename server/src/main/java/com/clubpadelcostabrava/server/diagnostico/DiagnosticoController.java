package com.clubpadelcostabrava.server.diagnostico;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expone una comprobación pública de que la API puede consultar
 * la base de datos y contar las pistas registradas.
 */
@RestController
public class DiagnosticoController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta la consulta de diagnóstico.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public DiagnosticoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Consulta el número de pistas y devuelve el estado del servidor.
     *
     * @return estado OK y número de pistas registradas
     */
    @GetMapping("/api/diagnostico")
    public Map<String, Object> diagnostico() {
        Long pistas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pista", Long.class);

        return Map.of("estado", "OK", "pistas", pistas);
    }
}
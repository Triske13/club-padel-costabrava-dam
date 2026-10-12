package com.clubpadelcostabrava.server.diagnostico;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiagnosticoController {

    private final JdbcTemplate jdbcTemplate;

    public DiagnosticoController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/diagnostico")
    public Map<String, Object> diagnostico() {
        Long pistas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pista", Long.class);

        return Map.of("estado", "OK", "pistas", pistas);
    }
}
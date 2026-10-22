package com.clubpadelcostabrava.server.calendario;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/** Consulta los festivos locales cargados en la base de datos. */
@Repository
public class FestivoRepository {

    private final JdbcTemplate jdbcTemplate;

    public FestivoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Indica si la fecha figura en la lista local de festivos. */
    public boolean esFestivo(LocalDate fecha) {
        Boolean resultado = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (SELECT 1 FROM festivo WHERE fecha = ?)
                        """,
                Boolean.class,
                fecha
        );
        return Boolean.TRUE.equals(resultado);
    }
}
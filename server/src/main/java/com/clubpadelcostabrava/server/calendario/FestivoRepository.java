package com.clubpadelcostabrava.server.calendario;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Consulta los festivos locales registrados en la base de datos.
 */
@Repository
public class FestivoRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public FestivoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Comprueba si una fecha figura en la tabla de festivos.
     *
     * @param fecha fecha que se desea comprobar
     * @return true si la fecha está registrada como festivo
     */
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
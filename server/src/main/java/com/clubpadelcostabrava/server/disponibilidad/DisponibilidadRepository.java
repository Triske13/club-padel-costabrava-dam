package com.clubpadelcostabrava.server.disponibilidad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Lee las pistas y las horas ocupadas sin consultar datos de los socios. */
@Repository
public class DisponibilidadRepository {

    private final JdbcTemplate jdbcTemplate;

    public DisponibilidadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PistaDatos(int id, String nombre) {}

    /** Devuelve las pistas en el orden en que se mostrarán. */
    public List<PistaDatos> listarPistas() {
        return jdbcTemplate.query(
                """
                SELECT id, nombre
                FROM pista
                ORDER BY id
                """,
                (rs, fila) -> new PistaDatos(
                        rs.getInt("id"),
                        rs.getString("nombre")
                )
        );
    }

    public record OcupacionDatos(int pistaId, LocalTime hora, String estado) {}

    /** Identifica cada hora ocupada como reserva o bloqueo. */
    public List<OcupacionDatos> listarOcupaciones(LocalDate fecha) {
        return jdbcTemplate.query(
                """
                SELECT pista_id, hora,
                        CASE
                            WHEN reserva_id IS NOT NULL THEN 'RESERVADA'
                            ELSE 'BLOQUEADA'
                        END AS estado
                FROM ocupacion
                WHERE fecha = ?
                """,
                (rs, fila) -> new OcupacionDatos(
                        rs.getInt("pista_id"),
                        rs.getObject("hora", LocalTime.class),
                        rs.getString("estado")
                ),
                fecha
        );
    }
}

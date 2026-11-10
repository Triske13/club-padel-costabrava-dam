package com.clubpadelcostabrava.server.disponibilidad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Consulta las pistas y sus horas ocupadas sin acceder
 * a los datos personales de los socios.
 */
@Repository
public class DisponibilidadRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public DisponibilidadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Datos de una pista necesarios para mostrar la disponibilidad.
     *
     * @param id     identificador de la pista
     * @param nombre nombre visible de la pista
     */
    public record PistaDatos(int id, String nombre) {
    }

    /**
     * Consulta todas las pistas ordenadas por identificador.
     *
     * @return pistas en el orden en que se mostrarán
     */
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

    /**
     * Hora ocupada de una pista y su tipo de ocupación.
     *
     * @param pistaId identificador de la pista
     * @param hora    hora de inicio ocupada
     * @param estado  RESERVADA si pertenece a una reserva;
     *                BLOQUEADA si pertenece a un bloqueo
     */
    public record OcupacionDatos(int pistaId, LocalTime hora, String estado) {
    }

    /**
     * Consulta las horas ocupadas en una fecha e identifica
     * si pertenecen a reservas o bloqueos.
     *
     * @param fecha fecha que se desea consultar
     * @return ocupaciones registradas para ese día
     */
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

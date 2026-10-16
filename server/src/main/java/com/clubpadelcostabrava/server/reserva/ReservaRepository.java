package com.clubpadelcostabrava.server.reserva;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Contiene las operaciones SQL sobre reservas usadas en este bloque. */
@Repository
public class ReservaRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Cancela las reservas confirmadas que aún no empiezan y libera sus ocupaciones. */
    public List<ReservaCancelada> cancelarFuturasDeSocio(int socioId, int adminId) {
        List<ReservaCancelada> canceladas = jdbcTemplate.query(
                """
                UPDATE reserva
                SET estado = 'CANCELADA',
                    cancelada_en = CURRENT_TIMESTAMP,
                    cancelada_por_cuenta_id = ?,
                    motivo_cancelacion = 'Cambio de estado del socio'
                WHERE socio_id = ?
                  AND estado = 'CONFIRMADA'
                  AND (fecha + hora_inicio) >
                      (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Madrid')
                RETURNING id, pista_id, fecha, hora_inicio, duracion
                """,
                (rs, fila) -> new ReservaCancelada(
                        rs.getInt("id"),
                        rs.getInt("pista_id"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getInt("duracion")
                ),
                adminId,
                socioId
        );

        // Cada ocupación eliminada devuelve esa hora de pista a la disponibilidad.
        for (ReservaCancelada reserva : canceladas) {
            jdbcTemplate.update(
                    "DELETE FROM ocupacion WHERE reserva_id = ?",
                    reserva.id()
            );
        }

        return canceladas;
    }

    /** Datos de las reservas canceladas necesarios para redactar el aviso. */
    public record ReservaCancelada(
            int id,
            int pistaId,
            LocalDate fecha,
            LocalTime horaInicio,
            int duracion
    ) {}
}

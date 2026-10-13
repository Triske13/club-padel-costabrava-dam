package com.clubpadelcostabrava.server.reserva;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public class ReservaRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

        for (ReservaCancelada reserva : canceladas) {
            jdbcTemplate.update(
                    "DELETE FROM ocupacion WHERE reserva_id = ?",
                    reserva.id()
            );
        }

        return canceladas;
    }

    public record ReservaCancelada(
            int id,
            int pistaId,
            LocalDate fecha,
            LocalTime horaInicio,
            int duracion
    ) {}
}

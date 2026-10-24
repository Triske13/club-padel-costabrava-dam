package com.clubpadelcostabrava.server.reserva;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Contiene las operaciones SQL sobre reservas usadas en este bloque. */
@Repository
public class ReservaRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Cuenta las reservas confirmadas cuyo inicio aún no ha llegado. */
    public long contarFuturasConfirmadas(int socioId, LocalDateTime ahora) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM reserva
                WHERE socio_id = ?
                  AND estado = 'CONFIRMADA'
                  AND (fecha + hora_inicio) > ?
                """,
                Long.class,
                socioId,
                ahora
        );

        if (total == null) {
            throw new IllegalStateException("La base de datos no devolvió el total de reservas.");
        }

        return total;
    }

    /** Detecta horas del socio ocupadas por otra reserva, en cualquier pista. */
    public boolean existeSolapeSocio(int socioId, LocalDate fecha, LocalTime inicio, LocalTime fin) {

        Boolean resultado = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM ocupacion o
                    JOIN reserva r ON r.id = o.reserva_id
                    WHERE r.socio_id = ?
                      AND r.estado = 'CONFIRMADA'
                      AND o.fecha = ?
                      AND o.hora >= ?
                      AND o.hora < ?
                )
                """,
                Boolean.class,
                socioId,
                fecha,
                inicio,
                fin
        );

        return Boolean.TRUE.equals(resultado);
    }

    /** Indica si alguna hora de la pista ya pertenece a una reserva o bloqueo. */
    public boolean existeOcupacionPista(int pistaId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Boolean resultado = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM ocupacion
                    WHERE pista_id = ?
                      AND fecha = ?
                      AND hora >= ?
                      AND hora < ?
                )
                """,
                Boolean.class,
                pistaId,
                fecha,
                inicio,
                fin
        );

        return Boolean.TRUE.equals(resultado);
    }

    /** Indica si existe la pista solicitada. */
    public boolean existePista(int pistaId) {
        Boolean resultado = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM pista
                    WHERE id = ?
                )
                """,
                Boolean.class,
                pistaId
        );

        return Boolean.TRUE.equals(resultado);
    }

    /** Inserta la reserva y devuelve el ID. */
    public int insertarReserva(CrearReservaPeticion datos, int socioId, int creadorCuentaId) {
        Integer id = jdbcTemplate.queryForObject(
                """
                INSERT INTO reserva (
                    socio_id, pista_id, fecha, hora_inicio,
                    duracion, creada_por_cuenta_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                socioId,
                datos.pistaId(),
                datos.fecha(),
                datos.horaInicio(),
                datos.duracionHoras(),
                creadorCuentaId
        );

        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvió el ID de la reserva.");
        }

        return id;
    }

    /** Registra una hora ocupada por la reserva. */
    public void insertarOcupacion(int pistaId, LocalDate fecha, LocalTime hora, int reservaId) {
        jdbcTemplate.update(
                """
                INSERT INTO ocupacion (pista_id, fecha, hora, reserva_id)
                VALUES (?, ?, ?, ?)
                """,
                pistaId,
                fecha,
                hora,
                reservaId
        );
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

        /** Cada ocupación eliminada devuelve esa hora de pista a la disponibilidad. */
        for (ReservaCancelada reserva : canceladas) {
            jdbcTemplate.update(
                    "DELETE FROM ocupacion WHERE reserva_id = ?",
                    reserva.id()
            );
        }

        return canceladas;
    }

    /** Busca una reserva con los nombres necesarios para la respuesta. */
    public Optional<ReservaDatos> buscarPorId(int reservaId) {
        return jdbcTemplate.query(
                """
                SELECT r.id,
                       r.socio_id,
                       s.nombre || ' ' || s.apellidos AS socio_nombre,
                       r.pista_id,
                       p.nombre AS pista_nombre,
                       r.fecha,
                       r.hora_inicio,
                       r.duracion,
                       r.estado,
                       r.creada_en,
                       r.cancelada_en,
                       c.rol AS cancelada_por,
                       r.motivo_cancelacion
                FROM reserva r
                JOIN socio s ON s.id = r.socio_id
                JOIN pista p ON p.id = r.pista_id
                LEFT JOIN cuenta c ON c.id = r.cancelada_por_cuenta_id
                WHERE r.id = ?
                """,
                (rs, fila) -> new ReservaDatos(
                        rs.getInt("id"),
                        rs.getInt("socio_id"),
                        rs.getString("socio_nombre"),
                        rs.getInt("pista_id"),
                        rs.getString("pista_nombre"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getInt("duracion"),
                        rs.getString("estado"),
                        rs.getObject("creada_en", OffsetDateTime.class),
                        rs.getObject("cancelada_en", OffsetDateTime.class),
                        rs.getString("cancelada_por"),
                        rs.getString("motivo_cancelacion")
                ),
                reservaId
        ).stream().findFirst();
    }

    /** Datos leídos de la base de datos para preparar una respuesta de reserva. */
    public record ReservaDatos(
            int id,
            int socioId,
            String socioNombre,
            int pistaId,
            String pistaNombre,
            LocalDate fecha,
            LocalTime horaInicio,
            int duracionHoras,
            String estado,
            OffsetDateTime creadaEn,
            OffsetDateTime canceladaEn,
            String canceladaPor,
            String motivoCancelacion
    ) {}

    /** Datos de las reservas canceladas necesarios para redactar el aviso. */
    public record ReservaCancelada(
            int id,
            int pistaId,
            LocalDate fecha,
            LocalTime horaInicio,
            int duracion
    ) {}

}

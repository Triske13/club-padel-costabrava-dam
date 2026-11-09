package com.clubpadelcostabrava.server.reserva;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
                this::mapearReserva,
                reservaId
        ).stream().findFirst();
    }

    /** Lee y bloquea una reserva mientras se decide su cancelación. */
    public Optional<ReservaDatos> buscarPorIdParaCancelar(int reservaId) {
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
                FOR UPDATE OF r
                """,
                this::mapearReserva,
                reservaId
        ).stream().findFirst();
    }

    public List<ReservaDatos> listarParaAdmin(LocalDate fecha, Integer pistaId, Integer socioId) {
        return listarParaAdmin(fecha, pistaId, socioId, null, null, 0L);
    }

    /** Consulta reservas y admite filtros y paginación opcionales. */
    public List<ReservaDatos> listarParaAdmin(LocalDate fecha, Integer pistaId, Integer socioId, LocalDate desde, Integer limite, long desplazamiento) {
        StringBuilder sql = new StringBuilder("""
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
            WHERE 1 = 1
            """);

        List<Object> parametros = new ArrayList<>();

        if (fecha != null) {
            sql.append(" AND r.fecha = ?");
            parametros.add(fecha);
        }
        if (desde != null) {
            sql.append(" AND r.fecha >= ?");
            parametros.add(desde);
        }
        if (pistaId != null) {
            sql.append(" AND r.pista_id = ?");
            parametros.add(pistaId);
        }
        if (socioId != null) {
            sql.append(" AND r.socio_id = ?");
            parametros.add(socioId);
        }

        if (desde != null) {
            sql.append(" ORDER BY r.fecha ASC, r.hora_inicio ASC, r.id ASC");
        } else {
            sql.append(" ORDER BY r.fecha DESC, r.hora_inicio DESC, r.id DESC");
        }

        if (limite != null) {
            sql.append(" LIMIT ? OFFSET ?");
            parametros.add(limite);
            parametros.add(desplazamiento);
        }

        return jdbcTemplate.query(
                sql.toString(),
                this::mapearReserva,
                parametros.toArray()
        );
    }

    /** Lista las reservas de un socio, incluidas las canceladas. */
    public List<ReservaDatos> listarPorSocio(int socioId) {
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
                WHERE r.socio_id = ?
                ORDER BY r.fecha DESC, r.hora_inicio DESC, r.id DESC
                """,
                this::mapearReserva,
                socioId
        );
    }

    /** Convierte una fila de la consulta en los datos de una reserva. */
    private ReservaDatos mapearReserva(ResultSet rs, int fila) throws SQLException {
        return new ReservaDatos(
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
        );
    }

    /** Marca una reserva como cancelada y registra quién y cuándo actuó. */
    public int marcarCancelada(int reservaId, int cuentaId, String motivo) {
        return jdbcTemplate.update(
                """
                UPDATE reserva
                SET estado = 'CANCELADA',
                    cancelada_en = CURRENT_TIMESTAMP,
                    cancelada_por_cuenta_id = ?,
                    motivo_cancelacion = ?
                WHERE id = ?
                  AND estado = 'CONFIRMADA'
                """,
                cuentaId,
                motivo,
                reservaId
        );
    }

    /** Libera todas las horas ocupadas por una reserva cancelada. */
    public int eliminarOcupaciones(int reservaId) {
        return jdbcTemplate.update(
                "DELETE FROM ocupacion WHERE reserva_id = ?",
                reservaId
        );
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

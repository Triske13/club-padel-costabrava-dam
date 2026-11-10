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

/**
 * Accede a las reservas y sus ocupaciones mediante consultas SQL.
 * Ofrece las operaciones que usan los servicios para comprobar disponibilidad,
 * guardar reservas, consultarlas y liberar horas al cancelarlas.
 */
@Repository
public class ReservaRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public ReservaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Cuenta las reservas confirmadas del socio cuyo inicio es posterior a ahora.
     * Una reserva de dos horas cuenta como una sola reserva.
     *
     * @param socioId identificador del socio
     * @param ahora   fecha y hora de referencia
     * @return número de reservas futuras confirmadas
     */
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

    /**
     * Comprueba si el socio ya tiene ocupada alguna hora del intervalo,
     * independientemente de la pista de su otra reserva.
     *
     * @param socioId identificador del socio
     * @param fecha   fecha solicitada
     * @param inicio  primera hora del intervalo, incluida
     * @param fin     hora final del intervalo, excluida
     * @return true si existe al menos una hora solapada
     */
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

    /**
     * Comprueba si una reserva o un bloqueo ocupa alguna hora de la pista
     * dentro del intervalo solicitado.
     *
     * @param pistaId identificador de la pista
     * @param fecha   fecha solicitada
     * @param inicio  primera hora del intervalo, incluida
     * @param fin     hora final del intervalo, excluida
     * @return true si alguna hora ya está ocupada
     */
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

    /**
     * Comprueba si existe una pista con el identificador indicado.
     *
     * @param pistaId identificador de la pista
     * @return true si la pista existe
     */
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

    /**
     * Inserta una reserva y devuelve el identificador asignado por PostgreSQL.
     * El servicio la llama dentro de la misma transacción que inserta sus ocupaciones.
     *
     * @param datos           pista, fecha, hora y duración de la reserva
     * @param socioId         identificador del socio titular
     * @param creadorCuentaId identificador de la cuenta que la crea
     * @return identificador de la nueva reserva
     * @throws IllegalStateException si la base de datos no devuelve el identificador
     */
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

    /**
     * Registra una de las horas ocupadas por una reserva.
     * La clave primaria de ocupacion impide duplicar la misma pista, fecha y hora.
     *
     * @param pistaId   identificador de la pista
     * @param fecha     fecha de la ocupación
     * @param hora      hora de inicio del turno
     * @param reservaId identificador de la reserva propietaria de la ocupación
     */
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

    /**
     * Cancela las reservas confirmadas del socio que aún no han comenzado
     * y elimina las ocupaciones de cada una.
     * Debe llamarse dentro de una transacción para que ambos cambios sean indivisibles.
     *
     * @param socioId identificador del socio que se inactiva
     * @param adminId identificador de la cuenta administradora que realiza el cambio
     * @return datos de las reservas canceladas para preparar el aviso por correo
     */
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

        // Al eliminar las ocupaciones, las horas vuelven a estar disponibles.
        for (ReservaCancelada reserva : canceladas) {
            jdbcTemplate.update(
                    "DELETE FROM ocupacion WHERE reserva_id = ?",
                    reserva.id()
            );
        }

        return canceladas;
    }

    /**
     * Busca una reserva con los nombres del socio y la pista
     * necesarios para construir la respuesta de la API.
     *
     * @param reservaId identificador de la reserva
     * @return la reserva encontrada, o un Optional vacío si no existe
     */
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

    /**
     * Busca una reserva y bloquea su fila hasta que termine la transacción.
     * Esto evita que dos operaciones decidan cancelarla simultáneamente.
     *
     * @param reservaId identificador de la reserva
     * @return la reserva encontrada, o un Optional vacío si no existe
     */
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

    /**
     * Lista las reservas de administración aplicando los filtros indicados,
     * sin paginación. Un filtro nulo no restringe ese campo.
     *
     * @param fecha   fecha concreta, o null para cualquier fecha
     * @param pistaId identificador de pista, o null para cualquier pista
     * @param socioId identificador de socio, o null para cualquier socio
     * @return reservas que cumplen los filtros
     */
    public List<ReservaDatos> listarParaAdmin(LocalDate fecha, Integer pistaId, Integer socioId) {
        return listarParaAdmin(fecha, pistaId, socioId, null, null, 0L);
    }

    /**
     * Consulta reservas con filtros y paginación opcionales.
     * Si se indica desde, ordena desde las fechas más próximas; en caso
     * contrario, muestra primero las reservas más recientes.
     *
     * @param fecha          fecha concreta, o null para cualquier fecha
     * @param pistaId        identificador de pista, o null para cualquier pista
     * @param socioId        identificador de socio, o null para cualquier socio
     * @param desde          fecha mínima incluida, o null si no hay límite inferior
     * @param limite         número máximo de filas, o null para no aplicar paginación
     * @param desplazamiento número de filas que se omiten al paginar
     * @return reservas que cumplen los filtros, en el orden indicado
     */
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

    /**
     * Lista todas las reservas de un socio, incluidas las canceladas,
     * ordenadas de la fecha más reciente a la más antigua.
     *
     * @param socioId identificador del socio
     * @return reservas del socio
     */
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

    /**
     * Convierte una fila SQL en los datos de una reserva.
     *
     * @param rs   fila devuelta por la consulta
     * @param fila índice de fila requerido por RowMapper
     * @return datos de la reserva
     * @throws SQLException si falla la lectura de alguna columna
     */
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

    /**
     * Cambia una reserva confirmada a CANCELADA y registra quién la canceló,
     * cuándo se canceló y el motivo. Este método no libera las ocupaciones.
     *
     * @param reservaId identificador de la reserva
     * @param cuentaId  identificador de la cuenta que cancela
     * @param motivo    motivo de la cancelación, o null si cancela el socio
     * @return número de filas actualizadas: uno si se canceló, cero si no
     */
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

    /**
     * Elimina todas las horas ocupadas por una reserva para liberarlas.
     * La fila de la reserva permanece en la base de datos como historial.
     *
     * @param reservaId identificador de la reserva
     * @return número de horas liberadas
     */
    public int eliminarOcupaciones(int reservaId) {
        return jdbcTemplate.update(
                "DELETE FROM ocupacion WHERE reserva_id = ?",
                reservaId
        );
    }

    /**
     * Datos de una reserva leídos de la base de datos para preparar la respuesta.
     *
     * @param id                identificador de la reserva
     * @param socioId           identificador del socio titular
     * @param socioNombre       nombre completo del socio
     * @param pistaId           identificador de la pista
     * @param pistaNombre       nombre de la pista
     * @param fecha             fecha de la reserva
     * @param horaInicio        hora de inicio
     * @param duracionHoras     duración en horas
     * @param estado            estado de la reserva
     * @param creadaEn          momento de creación
     * @param canceladaEn       momento de cancelación, si la hubo
     * @param canceladaPor      rol de la cuenta que canceló, si la hubo
     * @param motivoCancelacion motivo registrado, si lo hubo
     */
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
    ) {
    }

    /**
     * Datos mínimos de una reserva cancelada al inactivar a un socio.
     * Se utilizan para preparar el aviso resumen.
     *
     * @param id         identificador de la reserva
     * @param pistaId    identificador de la pista
     * @param fecha      fecha de la reserva
     * @param horaInicio hora de inicio
     * @param duracion   duración en horas
     */
    public record ReservaCancelada(
            int id,
            int pistaId,
            LocalDate fecha,
            LocalTime horaInicio,
            int duracion
    ) {
    }

}

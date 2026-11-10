package com.clubpadelcostabrava.server.bloqueo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Accede a los bloqueos de pista y a sus horas ocupadas en PostgreSQL.
 */
@Repository
public class BloqueoRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public BloqueoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Comprueba si existe la pista indicada.
     *
     * @param pistaId identificador de la pista
     * @return true si la pista existe
     */
    public boolean existePista(int pistaId) {
        Boolean resultado = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (SELECT 1 FROM pista WHERE id = ?)
                        """,
                Boolean.class,
                pistaId
        );
        return Boolean.TRUE.equals(resultado);
    }

    /**
     * Obtiene la fecha y hora de inicio de un bloqueo para comprobar
     * si todavía puede retirarse.
     *
     * @param id identificador del bloqueo
     * @return su inicio, o un Optional vacío si no existe
     */
    public Optional<LocalDateTime> buscarInicioPorId(int id) {
        return jdbcTemplate.query(
                """
                        SELECT fecha, hora_inicio
                        FROM bloqueo
                        WHERE id = ?
                        """,
                (rs, fila) -> LocalDateTime.of(
                        rs.getObject("fecha", LocalDate.class),
                        rs.getObject("hora_inicio", LocalTime.class)
                ),
                id
        ).stream().findFirst();
    }

    /**
     * Consulta los bloqueos de un día, ordenados por pista y hora de inicio.
     *
     * @param fecha fecha que se desea consultar
     * @return bloqueos de esa fecha
     */
    public List<BloqueoRespuesta> listarPorFecha(LocalDate fecha) {
        return jdbcTemplate.query(
                """
                        SELECT id, pista_id, fecha, hora_inicio, numero_horas, descripcion
                        FROM bloqueo
                        WHERE fecha = ?
                        ORDER BY pista_id, hora_inicio
                        """,
                (rs, fila) -> new BloqueoRespuesta(
                        rs.getInt("id"),
                        rs.getInt("pista_id"),
                        rs.getObject("fecha", LocalDate.class),
                        rs.getObject("hora_inicio", LocalTime.class),
                        rs.getInt("numero_horas"),
                        rs.getString("descripcion")
                ),
                fecha
        );
    }

    /**
     * Guarda un bloqueo y obtiene el identificador generado por PostgreSQL.
     * El servicio lo llama dentro de la transacción que ocupa sus horas.
     *
     * @param datos   datos del bloqueo
     * @param adminId cuenta administradora que lo crea
     * @return identificador del bloqueo creado
     * @throws IllegalStateException si la base de datos no devuelve el identificador
     */
    public int insertarBloqueo(CrearBloqueoPeticion datos, int adminId) {
        Integer id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO bloqueo (pista_id, fecha, hora_inicio, numero_horas, creada_por_cuenta_id, descripcion)
                        VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Integer.class,
                datos.pistaId(),
                datos.fecha(),
                datos.horaInicio(),
                datos.numeroHoras(),
                adminId,
                datos.descripcion()
        );
        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvió el ID.");
        }
        return id;
    }

    /**
     * Ocupa una hora de pista para el bloqueo indicado.
     * La clave primaria de ocupacion impide que esa misma pista, fecha
     * y hora pertenezca también a otra reserva o bloqueo.
     *
     * @param pistaId   identificador de la pista
     * @param fecha     fecha de la ocupación
     * @param hora      hora de inicio del turno
     * @param bloqueoId identificador del bloqueo
     */
    public void insertarOcupacion(int pistaId, LocalDate fecha, LocalTime hora, int bloqueoId) {
        jdbcTemplate.update(
                """
                        INSERT INTO ocupacion (pista_id, fecha, hora, bloqueo_id)
                        VALUES (?, ?, ?, ?)
                        """,
                pistaId,
                fecha,
                hora,
                bloqueoId
        );
    }

    /**
     * Elimina un bloqueo por su identificador.
     *
     * @param id identificador del bloqueo
     * @return número de filas eliminadas
     */
    public int eliminarPorId(int id) {
        return jdbcTemplate.update(
                "DELETE FROM bloqueo WHERE id = ?",
                id
        );
    }
}

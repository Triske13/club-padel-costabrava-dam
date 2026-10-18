package com.clubpadelcostabrava.server.bloqueo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Accede a los bloqueos y a sus horas ocupadas en la base de datos. */
@Repository
public class BloqueoRepository {

    private final JdbcTemplate jdbcTemplate;

    public BloqueoRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** Comprueba que la pista indicada existe. */
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

    /** Obtiene el inicio de un bloqueo para decidir si aún puede retirarse. */
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

    /** Lista los bloqueos de un día, ordenados por pista y hora. */
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

    /** Guarda el bloqueo y devuelve el ID generado por PostgreSQL. */
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

    /** Ocupa una hora; la clave única impide compartirla con otra reserva o bloqueo. */
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

    /** Elimina el bloqueo y devuelve el número de filas eliminadas. */
    public int eliminarPorId(int id) {
        return jdbcTemplate.update(
                "DELETE FROM bloqueo WHERE id = ?",
                id
        );
    }
}

package com.clubpadelcostabrava.server.socio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Ejecuta las consultas SQL de fichas de socios y sus cuentas USER. */
@Repository
public class SocioRepository {
    private final JdbcTemplate jdbcTemplate;

    public SocioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // El JOIN une la ficha con su cuenta; la contraseña solo indica si se activó.
    private static final String SELECT_SOCIO = """
        SELECT s.id, s.nombre, s.apellidos, s.telefono,
               s.direccion, s.poblacion, s.estado, c.correo,
               (c.contrasena_hash IS NOT NULL) AS cuenta_activada
        FROM socio s
        JOIN cuenta c ON c.socio_id = s.id
        WHERE c.rol = 'USER'
        """;

    /** Devuelve todas las fichas ordenadas por identificador. */
    public List<SocioRespuesta> listar() {
        return jdbcTemplate.query(
                SELECT_SOCIO + " ORDER BY s.id",
                this::mapear
        );
    }

    /** Filtra por ID exacto o por fragmentos del nombre y del correo. */
    public List<SocioRespuesta> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listar();
        }

        String termino = texto.strip().toLowerCase(Locale.ROOT);
        return jdbcTemplate.query(
                SELECT_SOCIO + """
                 AND (
                     CAST(s.id AS TEXT) = ?
                     OR strpos(lower(s.nombre || ' ' || s.apellidos), ?) > 0
                     OR strpos(lower(c.correo), ?) > 0
                 )
                 ORDER BY s.id
                 """,
                this::mapear,
                termino, termino, termino
        );
    }

    /** Bloquea la fila del socio durante un cambio de estado concurrente. */
    public Optional<SocioRespuesta> buscarPorIdParaCambio(int id) {
        return jdbcTemplate.query(
                SELECT_SOCIO + " AND s.id = ? FOR UPDATE OF s",
                this::mapear,
                id
        ).stream().findFirst();
    }

    /** Busca una ficha por ID sin bloquearla. */
    public Optional<SocioRespuesta> buscarPorId(int id) {
        return jdbcTemplate.query(
                SELECT_SOCIO + " AND s.id = ?",
                this::mapear,
                id
        ).stream().findFirst();
    }

    /** Inserta la ficha y devuelve el ID asignado por PostgreSQL. */
    public int insertarFicha(CrearSocioPeticion datos) {
        Integer id = jdbcTemplate.queryForObject(
                """
                INSERT INTO socio (nombre, apellidos, telefono, direccion, poblacion)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                datos.nombre(),
                datos.apellidos(),
                datos.telefono(),
                datos.direccion(),
                datos.poblacion()
        );
        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvió el ID.");
        }
        return id;
    }

    /** Crea la cuenta USER asociada, inicialmente sin contraseña. */
    public void insertarCuenta(int socioId, String correo) {
        jdbcTemplate.update(
                "INSERT INTO cuenta (socio_id, correo, rol) VALUES (?, ?, 'USER')",
                socioId,
                correo
        );
    }

    /** Guarda datos personales y devuelve el número de filas modificadas. */
    public int actualizarFicha(int id, EditarSocioPeticion datos) {
        return jdbcTemplate.update(
                """
                UPDATE socio
                SET nombre = ?, apellidos = ?, telefono = ?,
                    direccion = ?, poblacion = ?
                WHERE id = ?
                """,
                datos.nombre(),
                datos.apellidos(),
                datos.telefono(),
                datos.direccion(),
                datos.poblacion(),
                id
        );
    }

    /** Cambia el correo de la cuenta USER y devuelve su ID. */
    public Optional<Integer> actualizarCorreo(int socioId, String nuevoCorreo) {
        return jdbcTemplate.query(
                """
                UPDATE cuenta
                SET correo = ?
                WHERE socio_id = ? AND rol = 'USER'
                RETURNING id
                """,
                (rs, fila) -> rs.getInt("id"),
                nuevoCorreo,
                socioId
        ).stream().findFirst();
    }

    /** Actualiza ACTIVO o INACTIVO en la ficha del socio. */
    public int actualizarEstado(int socioId, String estado) {
        return jdbcTemplate.update(
                "UPDATE socio SET estado = ? WHERE id = ?",
                estado,
                socioId
        );
    }

    /** Convierte una fila SQL en la respuesta que recibirá el cliente. */
    private SocioRespuesta mapear(ResultSet rs, int fila) throws SQLException {
        return new SocioRespuesta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("apellidos"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getString("poblacion"),
                rs.getString("correo"),
                rs.getString("estado"),
                rs.getBoolean("cuenta_activada")
        );
    }
}
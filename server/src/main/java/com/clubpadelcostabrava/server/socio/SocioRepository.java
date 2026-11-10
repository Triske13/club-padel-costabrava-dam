package com.clubpadelcostabrava.server.socio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ejecuta las consultas SQL de las fichas de socios
 * y de sus cuentas con rol USER.
 */
@Repository
public class SocioRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
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

    /**
     * Consulta todas las fichas de socios, ordenadas por identificador.
     *
     * @return fichas con los datos de su cuenta USER asociada
     */
    public List<SocioRespuesta> listar() {
        return jdbcTemplate.query(
                SELECT_SOCIO + " ORDER BY s.id",
                this::mapear
        );
    }

    /**
     * Busca socios por identificador exacto o por un fragmento
     * del nombre completo o del correo, sin distinguir mayúsculas.
     * Si el texto está vacío, devuelve todas las fichas.
     *
     * @param texto criterio de búsqueda, o null para listar todos
     * @return fichas que cumplen el criterio
     */
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

    /**
     * Busca una ficha y bloquea su fila mientras dura la transacción.
     * Se usa para evitar cambios de estado concurrentes sobre el mismo socio.
     *
     * @param id identificador del socio
     * @return ficha encontrada, o un Optional vacío si no existe
     */
    public Optional<SocioRespuesta> buscarPorIdParaCambio(int id) {
        return jdbcTemplate.query(
                SELECT_SOCIO + " AND s.id = ? FOR UPDATE OF s",
                this::mapear,
                id
        ).stream().findFirst();
    }

    /**
     * Busca una ficha por identificador sin bloquear su fila.
     *
     * @param id identificador del socio
     * @return ficha encontrada, o un Optional vacío si no existe
     */
    public Optional<SocioRespuesta> buscarPorId(int id) {
        return jdbcTemplate.query(
                SELECT_SOCIO + " AND s.id = ?",
                this::mapear,
                id
        ).stream().findFirst();
    }

    /**
     * Inserta los datos personales de un socio.
     *
     * @param datos ficha validada del nuevo socio
     * @return identificador asignado por PostgreSQL
     * @throws IllegalStateException si la base de datos no devuelve el identificador
     */
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

    /**
     * Crea la cuenta USER asociada al socio, inicialmente sin contraseña.
     *
     * @param socioId identificador del socio
     * @param correo  correo normalizado de su cuenta
     */
    public void insertarCuenta(int socioId, String correo) {
        jdbcTemplate.update(
                "INSERT INTO cuenta (socio_id, correo, rol) VALUES (?, ?, 'USER')",
                socioId,
                correo
        );
    }

    /**
     * Actualiza los datos personales de una ficha sin modificar el correo.
     *
     * @param id    identificador del socio
     * @param datos nuevos datos personales validados
     * @return número de filas modificadas
     */
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

    /**
     * Cambia el correo de la cuenta USER de un socio.
     *
     * @param socioId     identificador del socio
     * @param nuevoCorreo nuevo correo normalizado
     * @return identificador de la cuenta actualizada,
     * o un Optional vacío si no existe
     */
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

    /**
     * Guarda el nuevo estado de la ficha del socio.
     *
     * @param socioId identificador del socio
     * @param estado  ACTIVO o INACTIVO
     * @return número de filas modificadas
     */
    public int actualizarEstado(int socioId, String estado) {
        return jdbcTemplate.update(
                "UPDATE socio SET estado = ? WHERE id = ?",
                estado,
                socioId
        );
    }

    /**
     * Convierte una fila SQL de socio y cuenta en la respuesta de la API.
     *
     * @param rs   fila devuelta por la consulta
     * @param fila índice de fila requerido por RowMapper
     * @return datos del socio y estado de activación de su cuenta
     * @throws SQLException si falla la lectura de alguna columna
     */
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
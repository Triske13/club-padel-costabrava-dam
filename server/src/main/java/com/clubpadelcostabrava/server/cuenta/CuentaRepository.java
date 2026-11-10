package com.clubpadelcostabrava.server.cuenta;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Ejecuta las consultas SQL de cuentas utilizadas
 * por la autenticación y la administración.
 */
@Repository
public class CuentaRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public CuentaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Busca una cuenta por correo. Si pertenece a un socio,
     * incorpora también el estado de su ficha.
     *
     * @param correo correo normalizado en minúsculas
     * @return datos de acceso, o un Optional vacío si no existe
     */
    public Optional<CuentaAcceso> buscarPorCorreo(String correo) {
        return jdbcTemplate.query(
                """
                        SELECT c.id, c.correo, c.contrasena_hash, c.rol,
                               s.estado AS estado_socio
                        FROM cuenta c
                        LEFT JOIN socio s ON s.id = c.socio_id
                        WHERE lower(c.correo) = ?
                        """,
                (rs, fila) -> new CuentaAcceso(
                        rs.getInt("id"),
                        rs.getString("correo"),
                        rs.getString("contrasena_hash"),
                        rs.getString("rol"),
                        rs.getString("estado_socio")
                ),
                correo
        ).stream().findFirst();
    }

    /**
     * Datos necesarios para comprobar las credenciales y el estado de una cuenta.
     *
     * @param id             identificador de la cuenta
     * @param correo         correo de acceso
     * @param contrasenaHash hash de la contraseña, o null si aún no está establecida
     * @param rol            ADMIN o USER
     * @param estadoSocio    estado de la ficha si es USER; null si es ADMIN
     */
    public record CuentaAcceso(
            int id,
            String correo,
            String contrasenaHash,
            String rol,
            String estadoSocio
    ) {
    }

    /**
     * Comprueba si una cuenta todavía puede utilizar una sesión.
     * Exige contraseña y, para USER, una ficha de socio ACTIVA.
     *
     * @param cuentaId identificador de la cuenta
     * @param rol      rol asociado a la sesión
     * @return true si la cuenta existe y sigue habilitada
     */
    public boolean cuentaHabilitada(int cuentaId, String rol) {
        Integer cantidad = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM cuenta c
                        LEFT JOIN socio s ON s.id = c.socio_id
                        WHERE c.id = ?
                          AND c.rol = ?
                          AND c.contrasena_hash IS NOT NULL
                          AND (c.rol = 'ADMIN' OR s.estado = 'ACTIVO')
                        """,
                Integer.class,
                cuentaId,
                rol
        );
        return cantidad != null && cantidad == 1;
    }

    /**
     * Obtiene el hash de la contraseña de una cuenta ADMIN.
     * Se utiliza para confirmar su identidad antes de cambiar un correo.
     *
     * @param cuentaId identificador de la cuenta administradora
     * @return hash encontrado, o un Optional vacío si no existe
     */
    public Optional<String> hashAdministrador(int cuentaId) {
        return jdbcTemplate.query(
                """
                        SELECT contrasena_hash
                        FROM cuenta
                        WHERE id = ? AND rol = 'ADMIN'
                        """,
                (rs, fila) -> rs.getString("contrasena_hash"),
                cuentaId
        ).stream().findFirst();
    }

    /**
     * Busca la cuenta USER asociada a una ficha de socio.
     *
     * @param socioId identificador del socio
     * @return identificador de la cuenta, o un Optional vacío si no existe
     */
    public Optional<Integer> idUsuarioPorSocio(int socioId) {
        return jdbcTemplate.query(
                "SELECT id FROM cuenta WHERE socio_id = ? AND rol = 'USER'",
                (rs, fila) -> rs.getInt("id"),
                socioId
        ).stream().findFirst();
    }

    /**
     * Obtiene el socio titular de una cuenta USER autenticada.
     *
     * @param cuentaId identificador de la cuenta
     * @return identificador del socio, o un Optional vacío si no existe
     */
    public Optional<Integer> socioIdPorCuenta(int cuentaId) {
        return jdbcTemplate.query(
                """
                        SELECT socio_id
                        FROM cuenta
                        WHERE id = ?
                          AND rol = 'USER'
                        """,
                (rs, fila) -> rs.getInt("socio_id"),
                cuentaId
        ).stream().findFirst();
    }

    /**
     * Elimina el código temporal que pueda tener pendiente una cuenta.
     *
     * @param cuentaId identificador de la cuenta
     */
    public void eliminarCodigoPendiente(int cuentaId) {
        jdbcTemplate.update(
                "DELETE FROM codigo_temporal WHERE cuenta_id = ?",
                cuentaId
        );
    }

    /**
     * Guarda el nuevo hash de contraseña de una cuenta.
     *
     * @param cuentaId       identificador de la cuenta
     * @param contrasenaHash nuevo hash de la contraseña
     */
    public void actualizarContrasenaHash(int cuentaId, String contrasenaHash) {
        jdbcTemplate.update(
                """
                        UPDATE cuenta
                        SET contrasena_hash = ?
                        WHERE id = ?
                        """,
                contrasenaHash,
                cuentaId
        );
    }

    /**
     * Obtiene el hash de una cuenta que ya tiene contraseña establecida.
     *
     * @param cuentaId identificador de la cuenta
     * @return hash encontrado, o un Optional vacío si la cuenta
     * no existe o aún no tiene contraseña
     */
    public Optional<String> buscarHashPorId(int cuentaId) {
        return jdbcTemplate.query(
                """
                        SELECT contrasena_hash
                        FROM cuenta
                        WHERE id = ? AND contrasena_hash IS NOT NULL
                        """,
                (rs, fila) -> rs.getString("contrasena_hash"),
                cuentaId
        ).stream().findFirst();
    }
}

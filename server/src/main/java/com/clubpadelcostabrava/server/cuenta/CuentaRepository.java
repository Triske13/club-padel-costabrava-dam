package com.clubpadelcostabrava.server.cuenta;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Ejecuta las consultas de cuentas usadas por el acceso y la administración. */
@Repository
public class CuentaRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Recibe JdbcTemplate para ejecutar SQL con parámetros. */
    public CuentaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Busca una cuenta y, si es USER, incorpora el estado de su ficha de socio. */
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

    /** Datos de cuenta necesarios para verificar credenciales y estado. */
    public record CuentaAcceso(
            int id,
            String correo,
            String contrasenaHash,
            String rol,
            String estadoSocio
    ) {}

    /** Comprueba en cada petición que la cuenta aún puede usar su token. */
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

    /** Obtiene el hash del ADMIN para confirmar su contraseña antes de cambiar un correo. */
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

    /** Localiza la cuenta USER asociada a una ficha de socio. */
    public Optional<Integer> idUsuarioPorSocio(int socioId) {
        return jdbcTemplate.query(
                "SELECT id FROM cuenta WHERE socio_id = ? AND rol = 'USER'",
                (rs, fila) -> rs.getInt("id"),
                socioId
        ).stream().findFirst();
    }

    /** Obtiene el socio asociado a una cuenta USER autenticada. */
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

    /** Anula el código temporal de una cuenta cuando deja de ser válido. */
    public void eliminarCodigoPendiente(int cuentaId) {
        jdbcTemplate.update(
                "DELETE FROM codigo_temporal WHERE cuenta_id = ?",
                cuentaId
        );
    }

    /** Guarda el nuevo hash de contraseña de una cuenta. */
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

    /** Obtiene el hash de la cuenta autenticada para comprobar su contraseña actual. */
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

package com.clubpadelcostabrava.server.cuenta;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CuentaRepository {

    private final JdbcTemplate jdbcTemplate;

    public CuentaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public record CuentaAcceso(
            int id,
            String correo,
            String contrasenaHash,
            String rol,
            String estadoSocio
    ) {}
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

    public Optional<Integer> idUsuarioPorSocio(int socioId) {
        return jdbcTemplate.query(
                "SELECT id FROM cuenta WHERE socio_id = ? AND rol = 'USER'",
                (rs, fila) -> rs.getInt("id"),
                socioId
        ).stream().findFirst();
    }

    public void eliminarCodigoPendiente(int cuentaId) {
        jdbcTemplate.update(
                "DELETE FROM codigo_temporal WHERE cuenta_id = ?",
                cuentaId
        );
    }
}

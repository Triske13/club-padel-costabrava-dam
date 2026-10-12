package com.clubpadelcostabrava.server.cuenta;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}

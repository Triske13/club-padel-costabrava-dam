package com.clubpadelcostabrava.server.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/** Guarda y consulta los códigos temporales asociados a las cuentas. */
@Repository
public class CodigoTemporalRepository {
    private final JdbcTemplate jdbcTemplate;

    /** Recibe JdbcTemplate para ejecutar las operaciones SQL. */
    public CodigoTemporalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Crea o sustituye el código pendiente y reinicia su caducidad e intentos. */
    public void guardar(int cuentaId, String codigoProtegido) {
        jdbcTemplate.update(
                """
                INSERT INTO codigo_temporal
                    (cuenta_id, codigo_protegido, creado_en, caduca_en, intentos)
                VALUES
                    (?, ?, CURRENT_TIMESTAMP,
                     CURRENT_TIMESTAMP + INTERVAL '15 minutes', 0)
                ON CONFLICT (cuenta_id) DO UPDATE SET
                    codigo_protegido = EXCLUDED.codigo_protegido,
                    creado_en = EXCLUDED.creado_en,
                    caduca_en = EXCLUDED.caduca_en,
                    intentos = 0
                """,
                cuentaId,
                codigoProtegido
        );
    }

    /**
     * Busca el código y bloquea su fila durante la transacción para evitar
     * que dos peticiones lo consuman al mismo tiempo.
     */
    public Optional<CodigoPendiente> buscarParaActualizar(int cuentaId) {
        return jdbcTemplate.query(
                """
                SELECT codigo_protegido, caduca_en, intentos
                FROM codigo_temporal
                WHERE cuenta_id = ?
                FOR UPDATE
                """,
                (rs, fila) -> new CodigoPendiente(
                        rs.getString("codigo_protegido"),
                        rs.getTimestamp("caduca_en").toInstant(),
                        rs.getInt("intentos")
                ),
                cuentaId
        ).stream().findFirst();
    }

    /** Registra un intento fallido de comprobación del código. */
    public void incrementarIntentos(int cuentaId) {
        jdbcTemplate.update(
                """
                        UPDATE codigo_temporal
                        SET intentos = intentos + 1
                        WHERE cuenta_id = ?
                        """,
                cuentaId
        );
    }

    /** Datos necesarios para comprobar un código solicitado anteriormente. */
    public record CodigoPendiente(
            String codigoProtegido,
            Instant caducaEn,
            int intentos
    ) {}

}

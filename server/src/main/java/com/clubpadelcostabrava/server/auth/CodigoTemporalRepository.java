package com.clubpadelcostabrava.server.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Guarda y consulta los códigos temporales asociados a cuentas.
 * En la base de datos se almacena el hash, nunca el código en claro.
 */
@Repository
public class CodigoTemporalRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Recibe el componente que ejecuta las consultas SQL.
     *
     * @param jdbcTemplate acceso JDBC a la base de datos
     */
    public CodigoTemporalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Crea o sustituye el código pendiente de una cuenta.
     * Fija una caducidad de 15 minutos y reinicia los intentos fallidos.
     *
     * @param cuentaId        identificador de la cuenta
     * @param codigoProtegido hash del nuevo código
     */
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
     * Busca el código pendiente y bloquea su fila hasta terminar
     * la transacción, para impedir que dos peticiones lo consuman a la vez.
     *
     * @param cuentaId identificador de la cuenta
     * @return código pendiente, o un Optional vacío si no existe
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

    /**
     * Suma un intento fallido al código pendiente de la cuenta.
     *
     * @param cuentaId identificador de la cuenta
     */
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

    /**
     * Datos almacenados para comprobar un código temporal.
     *
     * @param codigoProtegido hash del código
     * @param caducaEn        instante en que deja de ser válido
     * @param intentos        número de intentos fallidos registrados
     */
    public record CodigoPendiente(
            String codigoProtegido,
            Instant caducaEn,
            int intentos
    ) {
    }

}

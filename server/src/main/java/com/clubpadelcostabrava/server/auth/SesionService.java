package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crea y conserva en memoria sesiones identificadas por tokens aleatorios.
 * Cada sesión caduca a las ocho horas y se pierde al reiniciar el servidor.
 */
@Service
public class SesionService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    /**
     * Genera un token nuevo y lo asocia a una cuenta durante ocho horas.
     * También elimina las sesiones que ya habían caducado.
     *
     * @param cuentaId identificador de la cuenta autenticada
     * @param rol      rol de la cuenta
     * @return sesión creada con token y fecha de caducidad
     */
    public Sesion crear(int cuentaId, String rol) {
        Instant ahora = Instant.now();
        sesiones.entrySet().removeIf(
                entrada -> !entrada.getValue().expiraEn().isAfter(ahora)
        );

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Sesion sesion = new Sesion(
                token,
                cuentaId,
                rol,
                ahora.plus(8, ChronoUnit.HOURS)
        );
        sesiones.put(token, sesion);
        return sesion;
    }

    /**
     * Busca una sesión por token y elimina el token si ya ha caducado.
     *
     * @param token token Bearer recibido
     * @return sesión vigente, o un Optional vacío si no existe o ha caducado
     */
    public Optional<Sesion> buscar(String token) {
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return Optional.empty();
        }
        if (!sesion.expiraEn().isAfter(Instant.now())) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(sesion);
    }

    /**
     * Cierra una sesión concreta eliminando su token.
     *
     * @param token token de la sesión que se cierra
     */
    public void invalidar(String token) {
        sesiones.remove(token);
    }

    /**
     * Cierra todas las sesiones asociadas a una cuenta.
     *
     * @param cuentaId identificador de la cuenta
     */
    public void invalidarCuenta(int cuentaId) {
        sesiones.entrySet().removeIf(
                entrada -> entrada.getValue().cuentaId() == cuentaId
        );
    }

    /**
     * Datos que el servidor mantiene asociados a un token.
     * No incluye la contraseña de la cuenta.
     *
     * @param token    identificador aleatorio de la sesión
     * @param cuentaId identificador de la cuenta
     * @param rol      rol de la cuenta
     * @param expiraEn instante de caducidad
     */
    public record Sesion(
            String token,
            int cuentaId,
            String rol,
            Instant expiraEn
    ) {
    }
}

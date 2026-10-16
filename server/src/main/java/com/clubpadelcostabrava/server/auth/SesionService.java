package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Crea y conserva en memoria tokens de sesión opacos con caducidad de ocho horas. */
@Service
public class SesionService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    /** Genera un token aleatorio y lo asocia al ID, rol y caducidad de la cuenta. */
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

    /** Devuelve la sesión si existe y sigue vigente; elimina un token caducado. */
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

    /** Cierra una sesión concreta eliminando su token del mapa. */
    public void invalidar(String token) {
        sesiones.remove(token);
    }

    /** Cierra todas las sesiones de una cuenta, por ejemplo al cambiar su estado. */
    public void invalidarCuenta(int cuentaId) {
        sesiones.entrySet().removeIf(
                entrada -> entrada.getValue().cuentaId() == cuentaId
        );
    }

    /** Datos que el servidor asocia a un token; no contienen la contraseña. */
    public record Sesion(
            String token,
            int cuentaId,
            String rol,
            Instant expiraEn
    ) {}
}

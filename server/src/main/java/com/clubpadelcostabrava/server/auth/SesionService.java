package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SesionService {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

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

    public void invalidar(String token) {
        sesiones.remove(token);
    }

    public void invalidarCuenta(int cuentaId) {
        sesiones.entrySet().removeIf(
                entrada -> entrada.getValue().cuentaId() == cuentaId
        );
    }

    public record Sesion(
            String token,
            int cuentaId,
            String rol,
            Instant expiraEn
    ) {}
}

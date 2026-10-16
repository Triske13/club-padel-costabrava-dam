package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Controla en memoria las solicitudes de código por correo e IP.
 * Sus contadores se reinician al arrancar de nuevo el servidor.
 */
@Service
public class LimiteCodigo {
    private final Map<String, Deque<Instant>> solicitudesPorCorreo  = new HashMap<>();
    private final Map<String, Deque<Instant>> solicitudesPorIp  = new HashMap<>();

    /** Descarta las solicitudes anteriores al inicio de la ventana de tiempo. */
    private void eliminarAnterioresA(Deque<Instant> anterioresA, Instant limite) {
        while (!anterioresA.isEmpty() && anterioresA.peekFirst().isBefore(limite)) {
            anterioresA.removeFirst();
        }
    }

    /**
     * Cuenta la solicitud por IP y comprueba ambos límites.
     * Solo registra el correo cuando se permite solicitar otro código.
     */
    public synchronized Resultado registrarSolicitud(String correo, String ip) {
        Instant ahora = Instant.now();
        Instant haceUnaHora = ahora.minus(Duration.ofHours(1));
        Instant haceUnMinuto = ahora.minus(Duration.ofMinutes(1));
        Deque<Instant> solicitudesIp = solicitudesPorIp.get(ip);
        Deque<Instant> solicitudesCorreo = solicitudesPorCorreo.get(correo);

        if (solicitudesIp == null) {
            solicitudesIp = new ArrayDeque<>();
            solicitudesPorIp.put(ip, solicitudesIp);
        }

        eliminarAnterioresA(solicitudesIp, haceUnaHora);

        if (solicitudesIp.size() >= 30) {
            return Resultado.IP_LIMITADA;
        }

        solicitudesIp.addLast(ahora);

        if (solicitudesCorreo == null) {
            solicitudesCorreo = new ArrayDeque<>();
            solicitudesPorCorreo.put(correo, solicitudesCorreo);
        }

        eliminarAnterioresA(solicitudesCorreo, haceUnaHora);

        if (solicitudesCorreo.size() >= 3
                || (!solicitudesCorreo.isEmpty()
                    && solicitudesCorreo.peekLast().isAfter(haceUnMinuto))) {
            return Resultado.CORREO_LIMITADO;
        }

        solicitudesCorreo.addLast(ahora);

        return Resultado.PERMITIDA;
    }

    /** Indica si la solicitud se permite o qué límite se ha alcanzado. */
    public enum Resultado {
        PERMITIDA,
        CORREO_LIMITADO,
        IP_LIMITADA
    }
}

package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Limita en memoria las solicitudes de códigos temporales por correo e IP.
 * Los contadores se reinician al arrancar de nuevo el servidor.
 */
@Service
public class LimiteCodigo {
    private final Map<String, Deque<Instant>> solicitudesPorCorreo = new HashMap<>();
    private final Map<String, Deque<Instant>> solicitudesPorIp = new HashMap<>();

    /**
     * Elimina del inicio de la cola las solicitudes anteriores al límite.
     *
     * @param anterioresA cola de instantes ordenados del más antiguo al más reciente
     * @param limite      inicio de la ventana de tiempo que se conserva
     */
    private void eliminarAnterioresA(Deque<Instant> anterioresA, Instant limite) {
        while (!anterioresA.isEmpty() && anterioresA.peekFirst().isBefore(limite)) {
            anterioresA.removeFirst();
        }
    }

    /**
     * Registra la solicitud por IP y comprueba los límites.
     * Permite hasta 30 solicitudes por IP en una hora. Por correo permite
     * hasta tres en una hora, separadas al menos por un minuto.
     * Una solicitud limitada por correo sigue contando para la IP.
     *
     * @param correo correo normalizado al que se solicita el código
     * @param ip     dirección IP de la petición
     * @return PERMITIDA, CORREO_LIMITADO o IP_LIMITADA
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

    /**
     * Resultado de comprobar los límites de una solicitud de código.
     */
    public enum Resultado {
        PERMITIDA,
        CORREO_LIMITADO,
        IP_LIMITADA
    }
}

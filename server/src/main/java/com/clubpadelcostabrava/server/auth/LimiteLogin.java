package com.clubpadelcostabrava.server.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class LimiteLogin {

    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Estado> porCorreo = new HashMap<>();
    private final Map<String, Estado> porIp = new HashMap<>();

    public synchronized boolean estaPausado(String correo, String ip) {
        Instant ahora = Instant.now();
        return estaPausado(porCorreo, correo, ahora)
                || estaPausado(porIp, ip, ahora);
    }

    public synchronized boolean registrarFallo(String correo, String ip) {
        Instant ahora = Instant.now();
        boolean correoPausado = registrar(porCorreo, correo, 5, ahora);
        boolean ipPausada = registrar(porIp, ip, 50, ahora);
        return correoPausado || ipPausada;
    }

    public synchronized void reiniciarCorreo(String correo) {
        porCorreo.remove(correo);
    }

    private boolean estaPausado(
            Map<String, Estado> registros, String clave, Instant ahora
    ) {
        Estado estado = registros.get(clave);
        if (estado == null) {
            return false;
        }
        if (estado.pausaHasta != null && ahora.isBefore(estado.pausaHasta)) {
            return true;
        }
        estado.pausaHasta = null;
        limpiarFallos(estado, ahora);
        if (estado.fallos.isEmpty()) {
            registros.remove(clave);
        }
        return false;
    }

    private boolean registrar(
            Map<String, Estado> registros, String clave, int maximo, Instant ahora
    ) {
        Estado estado = registros.computeIfAbsent(clave, k -> new Estado());
        limpiarFallos(estado, ahora);
        estado.fallos.addLast(ahora);

        if (estado.fallos.size() >= maximo) {
            estado.pausaHasta = ahora.plus(VENTANA);
            return true;
        }
        return false;
    }

    private void limpiarFallos(Estado estado, Instant ahora) {
        Instant limite = ahora.minus(VENTANA);
        while (!estado.fallos.isEmpty()
                && !estado.fallos.peekFirst().isAfter(limite)) {
            estado.fallos.removeFirst();
        }
    }

    private static class Estado {
        private final Deque<Instant> fallos = new ArrayDeque<>();
        private Instant pausaHasta;
    }
}
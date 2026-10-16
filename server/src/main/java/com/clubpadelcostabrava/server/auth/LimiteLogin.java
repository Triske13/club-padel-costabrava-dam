package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Mantiene en memoria los fallos de acceso por correo e IP durante quince minutos. */
@Service
public class LimiteLogin {

    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Estado> porCorreo = new HashMap<>();
    private final Map<String, Estado> porIp = new HashMap<>();

    /** Indica si el correo o la IP siguen dentro de una pausa; no añade un fallo. */
    public synchronized boolean estaPausado(String correo, String ip) {
        Instant ahora = Instant.now();
        return estaPausado(porCorreo, correo, ahora)
                || estaPausado(porIp, ip, ahora);
    }

    /** Añade un fallo a ambos contadores y avisa si alguno alcanza su límite. */
    public synchronized boolean registrarFallo(String correo, String ip) {
        Instant ahora = Instant.now();
        boolean correoPausado = registrar(porCorreo, correo, 5, ahora);
        boolean ipPausada = registrar(porIp, ip, 50, ahora);
        return correoPausado || ipPausada;
    }

    /** Borra los fallos de ese correo tras un acceso correcto, sin borrar los de la IP. */
    public synchronized void reiniciarCorreo(String correo) {
        porCorreo.remove(correo);
    }

    /** Consulta un contador, descarta fallos antiguos y comprueba la pausa vigente. */
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

    /** Registra la hora del fallo y fija la pausa cuando se alcanza el máximo. */
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

    /** Elimina los intentos que han salido de la ventana de quince minutos. */
    private void limpiarFallos(Estado estado, Instant ahora) {
        Instant limite = ahora.minus(VENTANA);
        while (!estado.fallos.isEmpty()
                && !estado.fallos.peekFirst().isAfter(limite)) {
            estado.fallos.removeFirst();
        }
    }

    /** Guarda las horas de los fallos y, si procede, el fin de la pausa. */
    private static class Estado {
        private final Deque<Instant> fallos = new ArrayDeque<>();
        private Instant pausaHasta;
    }
}
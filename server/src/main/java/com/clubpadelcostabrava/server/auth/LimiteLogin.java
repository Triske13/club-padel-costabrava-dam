package com.clubpadelcostabrava.server.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Limita en memoria los intentos fallidos de login por correo e IP.
 * Los contadores y las pausas se pierden al reiniciar el servidor.
 */
@Service
public class LimiteLogin {

    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Estado> porCorreo = new HashMap<>();
    private final Map<String, Estado> porIp = new HashMap<>();

    /**
     * Comprueba si el correo o la IP tienen una pausa vigente.
     * Esta consulta no registra un nuevo fallo.
     *
     * @param correo correo normalizado
     * @param ip     dirección IP de la petición
     * @return true si alguno de los dos sigue temporalmente limitado
     */
    public synchronized boolean estaPausado(String correo, String ip) {
        Instant ahora = Instant.now();
        return estaPausado(porCorreo, correo, ahora)
                || estaPausado(porIp, ip, ahora);
    }

    /**
     * Registra un fallo para el correo y la IP.
     * El límite es de cinco fallos por correo o 50 por IP en 15 minutos.
     *
     * @param correo correo normalizado
     * @param ip     dirección IP de la petición
     * @return true si alguno de los contadores alcanzó su límite
     */
    public synchronized boolean registrarFallo(String correo, String ip) {
        Instant ahora = Instant.now();
        boolean correoPausado = registrar(porCorreo, correo, 5, ahora);
        boolean ipPausada = registrar(porIp, ip, 50, ahora);
        return correoPausado || ipPausada;
    }

    /**
     * Elimina los fallos del correo tras un acceso correcto.
     * No modifica el contador compartido por la IP.
     *
     * @param correo correo cuyos fallos se eliminan
     */
    public synchronized void reiniciarCorreo(String correo) {
        porCorreo.remove(correo);
    }

    /**
     * Consulta un contador, termina una pausa vencida y elimina fallos antiguos.
     *
     * @param registros contadores por correo o por IP
     * @param clave     correo o IP que se comprueba
     * @param ahora     instante de referencia
     * @return true si la pausa sigue vigente
     */
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

    /**
     * Añade un fallo al contador y fija una pausa de 15 minutos
     * cuando se alcanza el máximo indicado.
     *
     * @param registros contadores por correo o por IP
     * @param clave     correo o IP del intento
     * @param maximo    número de fallos que activa la pausa
     * @param ahora     instante del fallo
     * @return true si se alcanzó el límite
     */
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

    /**
     * Elimina los fallos situados fuera de la ventana de 15 minutos.
     *
     * @param estado contador que se limpia
     * @param ahora  instante de referencia
     */
    private void limpiarFallos(Estado estado, Instant ahora) {
        Instant limite = ahora.minus(VENTANA);
        while (!estado.fallos.isEmpty()
                && !estado.fallos.peekFirst().isAfter(limite)) {
            estado.fallos.removeFirst();
        }
    }

    /**
     * Conserva los instantes de los fallos y el fin de la pausa, si existe.
     */
    private static class Estado {
        private final Deque<Instant> fallos = new ArrayDeque<>();
        private Instant pausaHasta;
    }
}
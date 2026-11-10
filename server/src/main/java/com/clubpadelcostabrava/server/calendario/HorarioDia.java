package com.clubpadelcostabrava.server.calendario;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Horas de apertura y cierre del club para una fecha.
 *
 * @param apertura primera hora disponible
 * @param cierre   hora de cierre, no disponible como inicio de turno
 */
public record HorarioDia(LocalTime apertura, LocalTime cierre) {

    /**
     * Genera los inicios de los turnos de una hora entre apertura y cierre.
     *
     * @return lista de horas desde apertura, sin incluir cierre
     */
    public List<LocalTime> turnos() {

        List<LocalTime> horas = new ArrayList<>();

        for (LocalTime hora = apertura; hora.isBefore(cierre); hora = hora.plusHours(1)) {
            horas.add(hora);
        }

        return List.copyOf(horas);
    }

    /**
     * Comprueba si un bloque de horas completas cabe dentro del horario.
     * El inicio debe ser una hora en punto y el final no puede superar el cierre.
     *
     * @param inicio      hora de inicio del bloque
     * @param numeroHoras duración del bloque en horas
     * @return true si todo el bloque está dentro del horario
     */
    public boolean cabe(LocalTime inicio, int numeroHoras) {

        if (inicio == null || numeroHoras <= 0) {
            return false;
        }

        if (inicio.getMinute() != 0
                || inicio.getSecond() != 0
                || inicio.getNano() != 0) {
            return false;
        }

        long segundoInicio = inicio.toSecondOfDay();
        long segundoFinal = segundoInicio + numeroHoras * 3600L;

        return segundoInicio >= apertura.toSecondOfDay()
                && segundoFinal <= cierre.toSecondOfDay();
    }
}

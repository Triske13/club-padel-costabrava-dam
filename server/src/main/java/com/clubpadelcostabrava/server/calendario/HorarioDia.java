package com.clubpadelcostabrava.server.calendario;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Horas de apertura y cierre aplicables a una fecha. */
public record HorarioDia(LocalTime apertura, LocalTime cierre) {

    /** Devuelve la hora de inicio de cada turno de una hora. */
    public List<LocalTime> turnos() {

        List<LocalTime> horas = new ArrayList<>();

        for (LocalTime hora = apertura; hora.isBefore(cierre); hora = hora.plusHours(1)) {
            horas.add(hora);
        }

        return List.copyOf(horas);
    }

    /** Indica si un bloque de horas completas cabe dentro del horario. */
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

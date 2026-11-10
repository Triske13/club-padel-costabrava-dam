package com.clubpadelcostabrava.server.disponibilidad;

import java.time.LocalDate;
import java.util.List;

/**
 * Horario y disponibilidad de todas las pistas en una fecha.
 *
 * @param fecha              fecha consultada
 * @param horaApertura       hora de apertura en formato HH:mm
 * @param horaCierre         hora de cierre en formato HH:mm
 * @param dentroPlazoReserva indica si la fecha está dentro del plazo de reserva
 * @param pistas             disponibilidad de cada pista
 */
public record DisponibilidadDiaRespuesta(
        LocalDate fecha,
        String horaApertura,
        String horaCierre,
        boolean dentroPlazoReserva,
        List<PistaDisponibilidadRespuesta> pistas
) {
}

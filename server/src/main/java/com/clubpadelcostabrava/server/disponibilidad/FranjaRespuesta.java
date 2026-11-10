package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/**
 * Estado de una hora de pista y duraciones que pueden reservarse desde ella.
 *
 * @param horaInicio            hora de inicio en formato HH:mm
 * @param estado                LIBRE, RESERVADA, BLOQUEADA o PASADA
 * @param duracionesDisponibles horas reservables desde esa franja:
 *                              lista vacía, [1] o [1, 2]
 */
public record FranjaRespuesta(
        String horaInicio,
        String estado,
        List<Integer> duracionesDisponibles
) {
}

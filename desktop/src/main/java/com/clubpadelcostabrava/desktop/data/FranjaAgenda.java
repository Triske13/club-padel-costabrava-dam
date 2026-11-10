package com.clubpadelcostabrava.desktop.data;

import java.util.List;

/**
 * Estado de una hora de una pista mostrado en la agenda de administración.
 *
 * @param pistaId               identificador de la pista
 * @param pistaNombre           nombre de la pista
 * @param horaInicio            hora de inicio de la franja
 * @param estado                estado de ocupación de la franja
 * @param duracionesDisponibles duraciones que pueden comenzar en esa hora
 */
public record FranjaAgenda(
        int pistaId,
        String pistaNombre,
        String horaInicio,
        String estado,
        List<Integer> duracionesDisponibles
) {
}
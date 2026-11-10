package com.clubpadelcostabrava.desktop.data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bloqueo manual de una pista recibido de la API.
 *
 * @param id          identificador del bloqueo
 * @param pistaId     identificador de la pista bloqueada
 * @param fecha       fecha del bloqueo
 * @param horaInicio  hora en que comienza
 * @param numeroHoras número de horas consecutivas bloqueadas
 * @param descripcion motivo del bloqueo; puede ser null
 */
public record Bloqueo(
        int id,
        int pistaId,
        LocalDate fecha,
        LocalTime horaInicio,
        int numeroHoras,
        String descripcion
) {
}

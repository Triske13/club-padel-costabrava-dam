package com.clubpadelcostabrava.server.bloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de un bloqueo devueltos al crearlo o consultar una fecha.
 *
 * @param id          identificador del bloqueo
 * @param pistaId     identificador de la pista
 * @param fecha       fecha del bloqueo
 * @param horaInicio  hora de inicio del primer turno
 * @param numeroHoras número de horas consecutivas bloqueadas
 * @param descripcion descripción del bloqueo, si se indicó
 */
public record BloqueoRespuesta(Integer id, Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer numeroHoras, String descripcion) {
}

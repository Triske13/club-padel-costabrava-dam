package com.clubpadelcostabrava.server.bloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos enviados por administración para crear un bloqueo de pista.
 *
 * @param pistaId     identificador de la pista
 * @param fecha       fecha del bloqueo
 * @param horaInicio  hora de inicio del primer turno
 * @param numeroHoras número de horas consecutivas que se bloquearán
 * @param descripcion descripción opcional del motivo del bloqueo
 */
public record CrearBloqueoPeticion(Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer numeroHoras, String descripcion) {
}

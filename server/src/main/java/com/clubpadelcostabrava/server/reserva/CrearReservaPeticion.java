package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos solicitados al crear una reserva para el socio autenticado.
 *
 * @param pistaId       identificador de la pista
 * @param fecha         fecha de la reserva
 * @param horaInicio    hora de inicio del primer turno
 * @param duracionHoras duración solicitada, de una o dos horas
 */
public record CrearReservaPeticion(Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer duracionHoras) {
}

package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos enviados por administración para reservar en nombre de un socio.
 *
 * @param socioId       identificador del socio titular
 * @param pistaId       identificador de la pista
 * @param fecha         fecha de la reserva
 * @param horaInicio    hora de inicio del primer turno
 * @param duracionHoras duración solicitada, de una o dos horas
 */
public record CrearReservaAdminPeticion(Integer socioId, Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer duracionHoras) {
}

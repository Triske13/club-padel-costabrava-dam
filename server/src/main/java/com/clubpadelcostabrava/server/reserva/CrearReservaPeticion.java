package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

/**Datos enviados por un cliente para crear una reserva.*/
public record CrearReservaPeticion(Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer duracionHoras) {}

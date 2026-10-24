package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

public record CrearReservaAdminPeticion(Integer socioId, Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer duracionHoras) {}

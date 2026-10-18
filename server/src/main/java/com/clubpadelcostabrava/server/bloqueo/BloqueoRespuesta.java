package com.clubpadelcostabrava.server.bloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos de un bloqueo devueltos al consultar o crear bloqueos. */
public record BloqueoRespuesta(Integer id, Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer numeroHoras, String descripcion) {}

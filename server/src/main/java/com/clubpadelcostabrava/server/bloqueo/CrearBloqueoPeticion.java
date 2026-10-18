package com.clubpadelcostabrava.server.bloqueo;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos enviados por administración para bloquear horas de una pista. */
public record CrearBloqueoPeticion(Integer pistaId, LocalDate fecha, LocalTime horaInicio, Integer numeroHoras, String descripcion) {}

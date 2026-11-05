package com.clubpadelcostabrava.desktop.data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Bloqueo manual de una pista recibido de la API. */
public record Bloqueo(
        int id,
        int pistaId,
        LocalDate fecha,
        LocalTime horaInicio,
        int numeroHoras,
        String descripcion
) {}

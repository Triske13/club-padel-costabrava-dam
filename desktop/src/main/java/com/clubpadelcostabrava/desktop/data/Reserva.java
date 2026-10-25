package com.clubpadelcostabrava.desktop.data;

/** Datos de una reserva que muestra la aplicación de administración. */
public record Reserva(
        int id,
        int socioId,
        String socioNombre,
        int pistaId,
        String pistaNombre,
        String fecha,
        String horaInicio,
        String horaFin,
        int duracionHoras,
        String estado,
        boolean puedeCancelar
) {}

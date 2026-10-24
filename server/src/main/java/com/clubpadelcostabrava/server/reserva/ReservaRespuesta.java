package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Datos de una reserva que reciben los clientes. */
public record ReservaRespuesta(
        int id,
        int socioId,
        String socioNombre,
        int pistaId,
        String pistaNombre,
        LocalDate fecha,
        String horaInicio,
        String horaFin,
        int duracionHoras,
        String estado,
        OffsetDateTime creadaEn,
        OffsetDateTime canceladaEn,
        String canceladaPor,
        String motivoCancelacion,
        boolean puedeCancelar
) {}

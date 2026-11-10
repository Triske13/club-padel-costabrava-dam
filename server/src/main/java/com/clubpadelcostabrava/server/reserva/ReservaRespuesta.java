package com.clubpadelcostabrava.server.reserva;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Datos de una reserva enviados a los clientes Android y Windows.
 *
 * @param id                identificador de la reserva
 * @param socioId           identificador del socio titular
 * @param socioNombre       nombre completo del socio
 * @param pistaId           identificador de la pista
 * @param pistaNombre       nombre de la pista
 * @param fecha             fecha de la reserva
 * @param horaInicio        hora de inicio en formato HH:mm
 * @param horaFin           hora de finalización en formato HH:mm
 * @param duracionHoras     duración de la reserva en horas
 * @param estado            estado de la reserva
 * @param creadaEn          momento de creación
 * @param canceladaEn       momento de cancelación, si la hubo
 * @param canceladaPor      rol de la cuenta que canceló, si la hubo
 * @param motivoCancelacion motivo registrado, si lo hubo
 * @param puedeCancelar     indica si el usuario que recibe la respuesta puede cancelarla
 */
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
) {
}

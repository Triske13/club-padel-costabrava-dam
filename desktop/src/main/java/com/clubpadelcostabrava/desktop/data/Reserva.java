package com.clubpadelcostabrava.desktop.data;

/**
 * Datos de una reserva mostrada en la aplicación de administración.
 *
 * @param id            identificador de la reserva
 * @param socioId       identificador del socio titular
 * @param socioNombre   nombre del socio titular
 * @param pistaId       identificador de la pista reservada
 * @param pistaNombre   nombre de la pista reservada
 * @param fecha         fecha de la reserva
 * @param horaInicio    hora a la que comienza
 * @param horaFin       hora a la que termina
 * @param duracionHoras duración de la reserva en horas
 * @param estado        estado actual de la reserva
 * @param puedeCancelar indica si el socio puede cancelarla según la API
 */
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
) {
}

package com.clubpadelcostabrava.server.disponibilidad;

import java.time.LocalDate;

/**
 * Indicadores de disponibilidad de una fecha del calendario.
 *
 * @param fecha           día resumido
 * @param consultable     indica si el rol puede consultar ese día
 * @param completo        indica que hay turnos futuros, pero ninguno libre
 * @param sinHorasFuturas indica que hoy ya no quedan turnos por comenzar
 */
public record DiaCalendarioRespuesta(LocalDate fecha, boolean consultable, boolean completo, boolean sinHorasFuturas) {
}

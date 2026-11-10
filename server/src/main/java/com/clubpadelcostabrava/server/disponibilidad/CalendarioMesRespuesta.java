package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/**
 * Resumen de disponibilidad de todos los días de un mes.
 *
 * @param mes  mes consultado en formato AAAA-MM
 * @param dias indicadores de cada fecha del mes
 */
public record CalendarioMesRespuesta(String mes, List<DiaCalendarioRespuesta> dias) {
}

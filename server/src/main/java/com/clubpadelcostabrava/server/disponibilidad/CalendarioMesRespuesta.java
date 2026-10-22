package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/** Resumen de todas las fechas de un mes. */
public record CalendarioMesRespuesta(String mes, List<DiaCalendarioRespuesta> dias) {}

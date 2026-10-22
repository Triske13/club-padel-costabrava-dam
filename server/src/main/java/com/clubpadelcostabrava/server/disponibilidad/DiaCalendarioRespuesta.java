package com.clubpadelcostabrava.server.disponibilidad;

import java.time.LocalDate;

/** Indicadores de consulta y ocupación de una fecha del calendario. */
public record DiaCalendarioRespuesta(LocalDate fecha, boolean consultable, boolean completo, boolean sinHorasFuturas) {}

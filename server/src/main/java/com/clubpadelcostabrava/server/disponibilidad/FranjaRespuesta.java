package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/** Estado y duraciones reservables de una hora concreta. */
public record FranjaRespuesta(
        String horaInicio,
        String estado,
        List<Integer> duracionesDisponibles
) {}

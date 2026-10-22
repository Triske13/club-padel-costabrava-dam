package com.clubpadelcostabrava.server.disponibilidad;

import java.time.LocalDate;
import java.util.List;

/** Horario y disponibilidad de todas las pistas en una fecha. */
public record DisponibilidadDiaRespuesta(
        LocalDate fecha,
        String horaApertura,
        String horaCierre,
        boolean dentroPlazoReserva,
        List<PistaDisponibilidadRespuesta> pistas
) {}

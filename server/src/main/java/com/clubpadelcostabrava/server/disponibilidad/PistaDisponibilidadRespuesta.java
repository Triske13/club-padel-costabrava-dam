package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/** Franjas de disponibilidad de una pista durante un día. */
public record PistaDisponibilidadRespuesta(
        Integer id,
        String nombre,
        List<FranjaRespuesta> franjas
) {}

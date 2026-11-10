package com.clubpadelcostabrava.server.disponibilidad;

import java.util.List;

/**
 * Franjas de disponibilidad de una pista durante un día.
 *
 * @param id      identificador de la pista
 * @param nombre  nombre visible de la pista
 * @param franjas horas de apertura y estado de cada una
 */
public record PistaDisponibilidadRespuesta(
        Integer id,
        String nombre,
        List<FranjaRespuesta> franjas
) {
}

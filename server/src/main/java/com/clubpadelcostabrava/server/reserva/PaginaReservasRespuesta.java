package com.clubpadelcostabrava.server.reserva;

import java.util.List;

/**
 * Página de reservas enviada a administración.
 *
 * @param reservas reservas incluidas en la página, como máximo 50
 * @param hayMas   indica si existe una página posterior
 */
public record PaginaReservasRespuesta(List<ReservaRespuesta> reservas, boolean hayMas) {
}

package com.clubpadelcostabrava.desktop.data;

import java.util.List;

/**
 * Resultado de una consulta paginada de reservas.
 *
 * @param reservas reservas incluidas en la página solicitada
 * @param hayMas   indica si existe una página siguiente
 */
public record PaginaReservas(List<Reserva> reservas, boolean hayMas) {
}

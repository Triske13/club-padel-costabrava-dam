package com.clubpadelcostabrava.server.socio;

/**
 * Estado que administración solicita para la ficha de un socio.
 *
 * @param estado ACTIVO o INACTIVO
 */
public record CambiarEstadoPeticion(String estado) {
}

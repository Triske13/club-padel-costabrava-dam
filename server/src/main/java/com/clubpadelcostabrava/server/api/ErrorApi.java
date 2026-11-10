package com.clubpadelcostabrava.server.api;

/**
 * Error enviado por la API en las respuestas HTTP.
 *
 * @param codigo  identificador estable del tipo de error
 * @param mensaje explicación legible para el usuario
 */
public record ErrorApi(String codigo, String mensaje) {
}

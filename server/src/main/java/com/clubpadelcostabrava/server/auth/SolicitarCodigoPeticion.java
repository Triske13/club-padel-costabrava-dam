package com.clubpadelcostabrava.server.auth;

/**
 * Correo indicado para solicitar un código temporal.
 *
 * @param correo dirección a la que se solicita enviar el código
 */
public record SolicitarCodigoPeticion(String correo) {
}

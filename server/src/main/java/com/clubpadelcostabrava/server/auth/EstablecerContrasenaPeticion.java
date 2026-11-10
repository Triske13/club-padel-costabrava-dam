package com.clubpadelcostabrava.server.auth;

/**
 * Datos enviados para establecer o recuperar una contraseña.
 *
 * @param correo          correo de la cuenta
 * @param codigo          código temporal recibido
 * @param nuevaContrasena contraseña que se desea establecer
 */
public record EstablecerContrasenaPeticion(String correo, String codigo, String nuevaContrasena) {
}

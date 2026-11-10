package com.clubpadelcostabrava.server.cuenta;

/**
 * Contraseñas enviadas por la cuenta autenticada para cambiarlas.
 *
 * @param contrasenaActual contraseña que utiliza actualmente
 * @param nuevaContrasena  contraseña que desea establecer
 */
public record CambiarContrasenaPeticion(String contrasenaActual, String nuevaContrasena) {
}

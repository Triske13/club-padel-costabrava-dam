package com.clubpadelcostabrava.server.socio;

/**
 * Datos necesarios para cambiar el correo de un socio.
 *
 * @param nuevoCorreo     nueva dirección de correo
 * @param contrasenaAdmin contraseña de ADMIN para confirmar su identidad
 */
public record CambiarCorreoPeticion(
        String nuevoCorreo,
        String contrasenaAdmin
) {
}

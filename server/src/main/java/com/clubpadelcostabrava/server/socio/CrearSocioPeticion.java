package com.clubpadelcostabrava.server.socio;

/**
 * Datos enviados por administración para dar de alta a un socio.
 *
 * @param nombre    nombre del socio
 * @param apellidos apellidos del socio
 * @param telefono  teléfono de contacto
 * @param direccion dirección postal
 * @param poblacion población
 * @param correo    correo de la cuenta que se creará para el socio
 */
public record CrearSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo
) {
}

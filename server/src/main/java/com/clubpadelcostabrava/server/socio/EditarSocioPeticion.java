package com.clubpadelcostabrava.server.socio;

/**
 * Datos personales que administración puede editar en una ficha.
 * El correo se cambia mediante una operación distinta.
 *
 * @param nombre    nuevo nombre
 * @param apellidos nuevos apellidos
 * @param telefono  nuevo teléfono de contacto
 * @param direccion nueva dirección postal
 * @param poblacion nueva población
 */
public record EditarSocioPeticion(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion
) {
}

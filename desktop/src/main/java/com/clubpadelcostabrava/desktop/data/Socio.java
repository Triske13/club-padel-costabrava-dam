package com.clubpadelcostabrava.desktop.data;

/**
 * Ficha de un socio recibida de la API para mostrarla en administración.
 *
 * @param id             identificador del socio
 * @param nombre         nombre del socio
 * @param apellidos      apellidos del socio
 * @param telefono       teléfono de contacto
 * @param direccion      dirección postal
 * @param poblacion      población de residencia
 * @param correo         correo asociado a su cuenta
 * @param estado         estado actual del socio
 * @param cuentaActivada indica si la cuenta ya tiene contraseña
 */
public record Socio(
        int id,
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo,
        String estado,
        boolean cuentaActivada
) {
}
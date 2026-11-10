package com.clubpadelcostabrava.server.socio;

/**
 * Datos de la ficha de socio enviados a administración.
 *
 * @param id             identificador del socio
 * @param nombre         nombre del socio
 * @param apellidos      apellidos del socio
 * @param telefono       teléfono de contacto
 * @param direccion      dirección postal
 * @param poblacion      población
 * @param correo         correo de la cuenta asociada
 * @param estado         ACTIVO o INACTIVO
 * @param cuentaActivada indica si la cuenta ya tiene contraseña
 */
public record SocioRespuesta(
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

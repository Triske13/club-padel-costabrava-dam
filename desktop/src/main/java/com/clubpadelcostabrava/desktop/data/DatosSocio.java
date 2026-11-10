package com.clubpadelcostabrava.desktop.data;

/**
 * Datos recogidos del formulario para crear o editar una ficha de socio.
 *
 * @param nombre    nombre introducido
 * @param apellidos apellidos introducidos
 * @param telefono  teléfono de contacto
 * @param direccion dirección postal
 * @param poblacion población de residencia
 * @param correo    correo del socio; se utiliza al crear la ficha
 */
public record DatosSocio(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo
) {
}

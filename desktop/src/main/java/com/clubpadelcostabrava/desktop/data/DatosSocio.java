package com.clubpadelcostabrava.desktop.data;

/** Datos del formulario para crear o editar una ficha de socio. */
public record DatosSocio(
        String nombre,
        String apellidos,
        String telefono,
        String direccion,
        String poblacion,
        String correo
) {}

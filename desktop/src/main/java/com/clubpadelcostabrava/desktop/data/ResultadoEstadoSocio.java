package com.clubpadelcostabrava.desktop.data;

/**
 * Resultado del cambio de estado de un socio.
 *
 * @param estado             estado del socio después del cambio
 * @param reservasCanceladas número de reservas canceladas al inactivarlo
 * @param avisoCorreo        resultado del aviso enviado por correo
 */
public record ResultadoEstadoSocio(String estado, int reservasCanceladas, String avisoCorreo) {
}

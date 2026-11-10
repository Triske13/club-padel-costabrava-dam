package com.clubpadelcostabrava.desktop.data;

/**
 * Resultado de una cancelación realizada desde administración.
 *
 * @param reserva     reserva con su estado actualizado
 * @param avisoCorreo resultado del intento de enviar el aviso por correo
 */
public record ResultadoCancelacion(Reserva reserva, String avisoCorreo) {
}

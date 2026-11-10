package com.clubpadelcostabrava.server.socio;

/**
 * Resultado del cambio de estado de un socio.
 *
 * @param estado             estado resultante: ACTIVO o INACTIVO
 * @param reservasCanceladas número de reservas futuras canceladas
 * @param avisoCorreo        ACEPTADO o FALLIDO si se intentó enviar un aviso;
 *                           NO_APLICA si no había reservas que comunicar
 */
public record CambiarEstadoRespuesta(
        String estado,
        int reservasCanceladas,
        String avisoCorreo
) {
}

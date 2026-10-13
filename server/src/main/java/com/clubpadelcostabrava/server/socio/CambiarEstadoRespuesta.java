package com.clubpadelcostabrava.server.socio;

public record CambiarEstadoRespuesta(
        String estado,
        int reservasCanceladas,
        String avisoCorreo
) {
}

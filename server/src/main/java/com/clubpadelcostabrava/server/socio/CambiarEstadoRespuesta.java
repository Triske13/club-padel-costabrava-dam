package com.clubpadelcostabrava.server.socio;

/** Resume el nuevo estado, las cancelaciones y el resultado del aviso. */
public record CambiarEstadoRespuesta(
        String estado,
        int reservasCanceladas,
        String avisoCorreo
) {
}

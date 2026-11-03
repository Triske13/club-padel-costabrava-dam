package com.clubpadelcostabrava.desktop.data;

/** Una hora de una pista para mostrarla en la agenda. */
public record FranjaAgenda(
        int pistaId,
        String pistaNombre,
        String horaInicio,
        String estado
) {}
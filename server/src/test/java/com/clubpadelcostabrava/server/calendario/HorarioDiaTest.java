package com.clubpadelcostabrava.server.calendario;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioDiaTest {

    @Test
    void generaTurnosYRespetaLosLimitesDelHorario() {
        HorarioDia horario = new HorarioDia(
                LocalTime.of(8, 0),
                LocalTime.of(16, 0)
        );

        assertEquals(8, horario.turnos().size());
        assertEquals(LocalTime.of(8, 0), horario.turnos().get(0));
        assertEquals(LocalTime.of(15, 0), horario.turnos().get(7));

        assertTrue(horario.cabe(LocalTime.of(14, 0), 2));
        assertFalse(horario.cabe(LocalTime.of(15, 0), 2));
        assertFalse(horario.cabe(LocalTime.of(8, 30), 1));
    }
}

package com.clubpadelcostabrava.server.calendario;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HorarioServiceTest {

    @Test
    void aplicaElHorarioSegunElDiaYLosFestivos() {
        FestivoRepository festivos = mock(FestivoRepository.class);
        HorarioService servicio = new HorarioService(festivos);

        LocalDate lunes = LocalDate.of(2026, 10, 19);
        LocalDate sabado = LocalDate.of(2026, 10, 17);
        LocalDate domingo = LocalDate.of(2026, 10, 18);
        LocalDate festivo = LocalDate.of(2026, 12, 8);

        when(festivos.esFestivo(festivo)).thenReturn(true);

        assertEquals(LocalTime.of(8, 0), servicio.calcular(lunes).apertura());
        assertEquals(LocalTime.of(23, 0), servicio.calcular(lunes).cierre());
        assertEquals(LocalTime.of(20, 0), servicio.calcular(sabado).cierre());
        assertEquals(LocalTime.of(16, 0), servicio.calcular(domingo).cierre());
        assertEquals(LocalTime.of(16, 0), servicio.calcular(festivo).cierre());
    }
}

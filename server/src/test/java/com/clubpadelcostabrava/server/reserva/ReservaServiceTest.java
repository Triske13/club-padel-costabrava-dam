package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.calendario.HorarioService;
import com.clubpadelcostabrava.server.socio.SocioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ReservaServiceTest {

    private final ReservaService servicio = new ReservaService(
            mock(ReservaRepository.class),
            mock(SocioRepository.class),
            mock(HorarioService.class)
    );

    private ReservaRepository.ReservaDatos reserva(
            LocalDate fecha,
            LocalTime inicio,
            int duracion,
            OffsetDateTime creadaEn,
            String estado
    ) {
        return new ReservaRepository.ReservaDatos(
                1, 1, "Socio de prueba", 1, "Pista 1",
                fecha, inicio, duracion, estado, creadaEn,
                null, null, null
        );
    }

    @Test
    void socioRespetaLosDiezMinutosYElInicio() {
        ZonedDateTime ahora = ZonedDateTime.of(
                2026, 10, 26, 9, 0, 0, 0,
                ZoneId.of("Europe/Madrid")
        );

        var enElLimite = reserva(
                ahora.toLocalDate(),
                LocalTime.of(10, 0),
                1,
                ahora.minusMinutes(10).toOffsetDateTime(),
                "CONFIRMADA"
        );
        assertTrue(servicio.puedeCancelarSocio(enElLimite, ahora));

        var limiteSuperado = reserva(
                ahora.toLocalDate(),
                LocalTime.of(10, 0),
                1,
                ahora.minusMinutes(10).minusNanos(1).toOffsetDateTime(),
                "CONFIRMADA"
        );
        assertFalse(servicio.puedeCancelarSocio(limiteSuperado, ahora));

        var yaIniciada = reserva(
                ahora.toLocalDate(),
                LocalTime.of(9, 0),
                1,
                ahora.minusMinutes(5).toOffsetDateTime(),
                "CONFIRMADA"
        );
        assertFalse(servicio.puedeCancelarSocio(yaIniciada, ahora));
    }

    @Test
    void socioPuedeCancelarHastaElDiaAnterior() {
        ZonedDateTime ahora = ZonedDateTime.of(
                2026, 10, 26, 18, 0, 0, 0,
                ZoneId.of("Europe/Madrid")
        );

        var futura = reserva(
                ahora.toLocalDate().plusDays(1),
                LocalTime.of(8, 0),
                1,
                ahora.minusDays(1).toOffsetDateTime(),
                "CONFIRMADA"
        );

        assertTrue(servicio.puedeCancelarSocio(futura, ahora));
    }

    @Test
    void adminPuedeCancelarEnCursoPeroNoTerminada() {
        ZonedDateTime ahora = ZonedDateTime.of(
                2026, 10, 26, 10, 0, 0, 0,
                ZoneId.of("Europe/Madrid")
        );

        var enCurso = reserva(
                ahora.toLocalDate(), LocalTime.of(9, 0), 2,
                ahora.minusDays(1).toOffsetDateTime(), "CONFIRMADA"
        );
        var terminada = reserva(
                ahora.toLocalDate(), LocalTime.of(8, 0), 2,
                ahora.minusDays(1).toOffsetDateTime(), "CONFIRMADA"
        );

        assertTrue(servicio.puedeCancelarAdmin(enCurso, ahora));
        assertFalse(servicio.puedeCancelarAdmin(terminada, ahora));
    }
}

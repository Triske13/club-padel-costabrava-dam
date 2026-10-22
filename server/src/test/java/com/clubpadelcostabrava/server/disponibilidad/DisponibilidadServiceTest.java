package com.clubpadelcostabrava.server.disponibilidad;

import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisponibilidadServiceTest {

    @Test
    void unSocioNoConsultaFueraDeLasCatorceFechas() {
        DisponibilidadRepository repositorio =
                mock(DisponibilidadRepository.class);
        HorarioService horarios = mock(HorarioService.class);
        DisponibilidadService servicio =
                new DisponibilidadService(repositorio, horarios);

        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDate ultimoDiaPermitido = hoy.plusDays(13);
        LocalDate primerDiaFuera = hoy.plusDays(14);

        when(horarios.calcular(ultimoDiaPermitido)).thenReturn(
                new HorarioDia(LocalTime.of(8, 0), LocalTime.of(9, 0))
        );
        when(repositorio.listarPistas()).thenReturn(List.of(
                new DisponibilidadRepository.PistaDatos(1, "Pista 1")
        ));
        when(repositorio.listarOcupaciones(ultimoDiaPermitido))
                .thenReturn(List.of());

        DisponibilidadDiaRespuesta dentro =
                servicio.consultarDia(ultimoDiaPermitido, false);

        assertTrue(dentro.dentroPlazoReserva());

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultarDia(primerDiaFuera, false)
        );
    }

    @Test
    void marcaCompletoCuandoTodasLasPistasEstanOcupadas() {
        DisponibilidadRepository repositorio =
                mock(DisponibilidadRepository.class);
        HorarioService horarios = mock(HorarioService.class);
        DisponibilidadService servicio =
                new DisponibilidadService(repositorio, horarios);

        LocalDate manana = LocalDate.now(ZoneId.of("Europe/Madrid"))
                .plusDays(1);
        LocalTime hora = LocalTime.of(8, 0);

        when(horarios.calcular(any(LocalDate.class))).thenReturn(
                new HorarioDia(hora, LocalTime.of(9, 0))
        );

        when(repositorio.listarPistas()).thenReturn(List.of(
                new DisponibilidadRepository.PistaDatos(1, "Pista 1"),
                new DisponibilidadRepository.PistaDatos(2, "Pista 2"),
                new DisponibilidadRepository.PistaDatos(3, "Pista 3"),
                new DisponibilidadRepository.PistaDatos(4, "Pista 4")
        ));

        when(repositorio.listarOcupaciones(any(LocalDate.class)))
                .thenReturn(List.of());

        when(repositorio.listarOcupaciones(manana)).thenReturn(List.of(
                new DisponibilidadRepository.OcupacionDatos(1, hora, "BLOQUEADA"),
                new DisponibilidadRepository.OcupacionDatos(2, hora, "BLOQUEADA"),
                new DisponibilidadRepository.OcupacionDatos(3, hora, "BLOQUEADA"),
                new DisponibilidadRepository.OcupacionDatos(4, hora, "BLOQUEADA")
        ));

        CalendarioMesRespuesta calendario =
                servicio.consultarMes(YearMonth.from(manana), false);

        DiaCalendarioRespuesta dia = calendario.dias().stream()
                .filter(d -> d.fecha().equals(manana))
                .findFirst()
                .orElseThrow();

        assertTrue(dia.consultable());
        assertTrue(dia.completo());
        assertFalse(dia.sinHorasFuturas());
    }
}

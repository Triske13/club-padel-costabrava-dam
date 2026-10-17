package com.clubpadelcostabrava.server.calendario;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Calcula el horario completo del club */
@Service
public class HorarioService {

    private final FestivoRepository festivoRepository;

    public HorarioService(FestivoRepository festivoRepository) {
        this.festivoRepository = festivoRepository;
    }

    /** Calcula el horario aplicable a la fecha indicada. */
    public HorarioDia calcular(LocalDate fecha) {

        LocalTime apertura = LocalTime.of(8, 0);
        LocalTime cierre;

        if (festivoRepository.esFestivo(fecha) || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {

            cierre = LocalTime.of(16, 0);

        } else if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY) {

            cierre = LocalTime.of(20, 0);

        } else {

            cierre = LocalTime.of(23, 0);

        }

        return new HorarioDia(apertura, cierre);

    }
}

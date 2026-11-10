package com.clubpadelcostabrava.server.calendario;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Calcula el horario diario según el día de la semana y los festivos.
 * Un festivo utiliza el horario de domingo.
 */
@Service
public class HorarioService {

    private final FestivoRepository festivoRepository;

    /**
     * Recibe el repositorio de festivos locales.
     *
     * @param festivoRepository acceso a las fechas festivas
     */
    public HorarioService(FestivoRepository festivoRepository) {
        this.festivoRepository = festivoRepository;
    }

    /**
     * Calcula el horario aplicable a una fecha.
     * La apertura es a las 08:00; el cierre es a las 23:00 entre semana,
     * a las 20:00 el sábado y a las 16:00 el domingo o un festivo.
     *
     * @param fecha fecha para la que se calcula el horario
     * @return horas de apertura y cierre de ese día
     */
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

package com.clubpadelcostabrava.server.disponibilidad;

import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula la disponibilidad diaria y el resumen mensual del calendario.
 * Combina el horario de apertura con las ocupaciones de cada pista.
 */
@Service
public class DisponibilidadService {

    private final DisponibilidadRepository repositorio;
    private final HorarioService horarios;

    /**
     * Recibe el acceso a pistas y ocupaciones y el cálculo de horarios.
     *
     * @param repositorio consultas de pistas y ocupaciones
     * @param horarios    cálculo del horario aplicable a una fecha
     */
    public DisponibilidadService(DisponibilidadRepository repositorio, HorarioService horarios) {
        this.repositorio = repositorio;
        this.horarios = horarios;
    }

    /**
     * Comprueba si una fecha está entre hoy y los 13 días siguientes,
     * con ambos extremos incluidos.
     *
     * @param fecha fecha que se comprueba
     * @param hoy   fecha actual de referencia
     * @return true si la fecha está dentro del plazo de reserva
     */
    private boolean dentroPlazo(LocalDate fecha, LocalDate hoy) {

        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(13))) {
            return false;
        }

        return true;

    }

    /**
     * Construye las franjas de todas las pistas para una fecha, sin mostrar
     * datos de los titulares de las reservas.
     * USER solo puede consultar el plazo de reserva; ADMIN puede consultar
     * cualquier fecha que no sea pasada.
     *
     * @param fecha   fecha que se desea consultar
     * @param esAdmin indica si la consulta procede de administración
     * @return horario, pistas, estados y duraciones disponibles de ese día
     * @throws IllegalArgumentException si falta la fecha o está fuera
     *                                  del plazo de consulta del usuario
     */
    public DisponibilidadDiaRespuesta consultarDia(LocalDate fecha, boolean esAdmin) {

        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        LocalDate hoy = ahora.toLocalDate();

        if (esAdmin && fecha.isBefore(hoy)) {
            throw new IllegalArgumentException(
                    "No se pueden consultar fechas pasadas."
            );
        }

        if (!esAdmin && !dentroPlazo(fecha, hoy)) {
            throw new IllegalArgumentException(
                    "La fecha está fuera del plazo de consulta."
            );
        }

        HorarioDia horario = horarios.calcular(fecha);
        List<DisponibilidadRepository.PistaDatos> pistas = repositorio.listarPistas();
        List<DisponibilidadRepository.OcupacionDatos> ocupaciones =
                repositorio.listarOcupaciones(fecha);

        boolean dentroPlazoReserva = dentroPlazo(fecha, hoy);
        List<PistaDisponibilidadRespuesta> pistasRespuesta = new ArrayList<>();

        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        for (DisponibilidadRepository.PistaDatos pista : pistas) {
            List<FranjaRespuesta> franjas = new ArrayList<>();

            for (LocalTime hora : horario.turnos()) {

                String estado = estadoFranja(pista.id(), fecha, hora, ahora, ocupaciones);

                List<Integer> duraciones = calcularDuraciones(pista.id(), fecha, hora, ahora, horario, dentroPlazoReserva, ocupaciones);

                franjas.add(new FranjaRespuesta(hora.format(formatoHora), estado, duraciones));
            }

            pistasRespuesta.add(new PistaDisponibilidadRespuesta(
                    pista.id(),
                    pista.nombre(),
                    franjas
            ));
        }
        return new DisponibilidadDiaRespuesta(
                fecha,
                horario.apertura().format(formatoHora),
                horario.cierre().format(formatoHora),
                dentroPlazoReserva,
                List.copyOf(pistasRespuesta)
        );
    }

    /**
     * Resume cada fecha del mes según el rol y las horas futuras libres.
     *
     * @param mes     mes que se desea consultar
     * @param esAdmin indica si la consulta procede de administración
     * @return días del mes con sus indicadores de disponibilidad
     * @throws IllegalArgumentException si no se indica un mes
     */
    public CalendarioMesRespuesta consultarMes(YearMonth mes, boolean esAdmin) {
        if (mes == null) {
            throw new IllegalArgumentException("El mes es obligatorio.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        LocalDate hoy = ahora.toLocalDate();
        List<DiaCalendarioRespuesta> dias = new ArrayList<>();
        List<DisponibilidadRepository.PistaDatos> pistas = repositorio.listarPistas();

        for (int numero = 1; numero <= mes.lengthOfMonth(); numero++) {
            LocalDate fecha = mes.atDay(numero);

            boolean consultable = esAdmin
                    ? !fecha.isBefore(hoy)
                    : dentroPlazo(fecha, hoy);

            dias.add(resumirDia(fecha, consultable, ahora, pistas));
        }

        return new CalendarioMesRespuesta(
                mes.toString(),
                List.copyOf(dias)
        );
    }

    /**
     * Calcula si un día es consultable, está completo o ya no tiene
     * turnos futuros por comenzar.
     *
     * @param fecha       día que se resume
     * @param consultable indica si el rol puede consultar ese día
     * @param ahora       fecha y hora de referencia
     * @param pistas      pistas existentes
     * @return indicadores del día para el calendario
     */
    private DiaCalendarioRespuesta resumirDia(
            LocalDate fecha,
            boolean consultable,
            LocalDateTime ahora,
            List<DisponibilidadRepository.PistaDatos> pistas
    ) {
        if (!consultable) {
            return new DiaCalendarioRespuesta(fecha, false, false, false);
        }

        HorarioDia horario = horarios.calcular(fecha);
        List<DisponibilidadRepository.OcupacionDatos> ocupaciones = repositorio.listarOcupaciones(fecha);

        boolean hayTurnoFuturo = false;
        boolean hayHoraLibre = false;

        for (LocalTime hora : horario.turnos()) {
            if (!LocalDateTime.of(fecha, hora).isAfter(ahora)) {
                continue;
            }

            hayTurnoFuturo = true;

            for (DisponibilidadRepository.PistaDatos pista : pistas) {
                if ("LIBRE".equals(estadoFranja(
                        pista.id(), fecha, hora, ahora, ocupaciones))) {
                    hayHoraLibre = true;
                    break;
                }
            }
        }

        boolean sinHorasFuturas =
                fecha.equals(ahora.toLocalDate()) && !hayTurnoFuturo;
        boolean completo = hayTurnoFuturo && !hayHoraLibre;

        return new DiaCalendarioRespuesta(
                fecha,
                true,
                completo,
                sinHorasFuturas
        );
    }

    /**
     * Obtiene el estado de una hora de pista.
     * Si existe una ocupación, conserva RESERVADA o BLOQUEADA aunque
     * la hora ya haya comenzado; en otro caso devuelve PASADA o LIBRE.
     *
     * @param pistaId     identificador de la pista
     * @param fecha       fecha de la franja
     * @param hora        hora de inicio
     * @param ahora       fecha y hora de referencia
     * @param ocupaciones ocupaciones registradas para ese día
     * @return estado de la franja
     */
    private String estadoFranja(
            int pistaId,
            LocalDate fecha,
            LocalTime hora,
            LocalDateTime ahora,
            List<DisponibilidadRepository.OcupacionDatos> ocupaciones
    ) {
        for (DisponibilidadRepository.OcupacionDatos ocupacion : ocupaciones) {

            if (ocupacion.pistaId() == pistaId && ocupacion.hora().equals(hora)) {
                return ocupacion.estado();
            }
        }

        LocalDateTime inicio = LocalDateTime.of(fecha, hora);

        if (!inicio.isAfter(ahora)) {
            return "PASADA";
        }

        return "LIBRE";
    }

    /**
     * Determina si desde una franja pueden reservarse una o dos horas.
     * Comprueba el plazo, el horario y que todas las horas estén libres.
     *
     * @param pistaId     identificador de la pista
     * @param fecha       fecha de la franja
     * @param hora        hora de inicio
     * @param ahora       fecha y hora de referencia
     * @param horario     horario aplicable al día
     * @param dentroPlazo indica si la fecha está dentro del plazo de reserva
     * @param ocupaciones ocupaciones registradas para ese día
     * @return lista vacía, [1] o [1, 2] según las duraciones posibles
     */
    private List<Integer> calcularDuraciones(
            int pistaId,
            LocalDate fecha,
            LocalTime hora,
            LocalDateTime ahora,
            HorarioDia horario,
            boolean dentroPlazo,
            List<DisponibilidadRepository.OcupacionDatos> ocupaciones
    ) {
        if (!dentroPlazo) {
            return List.of();
        }

        if (!"LIBRE".equals(estadoFranja(pistaId, fecha, hora, ahora, ocupaciones))) {
            return List.of();
        }

        if (!horario.cabe(hora, 1)) {
            return List.of();
        }

        if (horario.cabe(hora, 2)
                && "LIBRE".equals(estadoFranja(
                pistaId, fecha, hora.plusHours(1), ahora, ocupaciones))) {
            return List.of(1, 2);
        }

        return List.of(1);

    }
}

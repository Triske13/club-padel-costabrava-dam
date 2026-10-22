package com.clubpadelcostabrava.server.disponibilidad;

import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Calcula el detalle diario y el resumen mensual de disponibilidad. */
@Service
public class DisponibilidadService {

    private final DisponibilidadRepository repositorio;
    private final HorarioService horarios;


    public DisponibilidadService(DisponibilidadRepository repositorio, HorarioService horarios) {
        this.repositorio = repositorio;
        this.horarios = horarios;
    }

    // Devuelve true desde hoy hasta hoy + 13 días, ambos incluidos.
    private boolean dentroPlazo(LocalDate fecha, LocalDate hoy) {

        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(13))) {
            return false;
        }

        return true;

    }

    /** Construye las franjas de las cuatro pistas sin mostrar datos de los titulares. */
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

                franjas.add(new FranjaRespuesta( hora.format(formatoHora), estado, duraciones));
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

    /** Resume todas las fechas del mes según el rol y las horas futuras libres. */
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

    /** Distingue un día completo de hoy sin turnos futuros por comenzar. */
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

    /** Conserva el estado de una ocupación aunque su hora ya haya comenzado. */
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

    /** Ofrece una o dos horas solo si caben, son futuras y están libres. */
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

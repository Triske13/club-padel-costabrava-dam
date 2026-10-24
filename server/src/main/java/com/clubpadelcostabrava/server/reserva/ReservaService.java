package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.api.ConflictoApiException;
import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import com.clubpadelcostabrava.server.socio.SocioRepository;
import com.clubpadelcostabrava.server.socio.SocioRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final SocioRepository socios;
    private final HorarioService horarios;

    public ReservaService(ReservaRepository reservas, SocioRepository socios, HorarioService horarios) {
        this.reservas = reservas;
        this.socios = socios;
        this.horarios = horarios;
    }

    private void validarDatos(CrearReservaPeticion datos) {

        if (datos == null
                || datos.pistaId() == null
                || datos.fecha() == null
                || datos.horaInicio() == null
                || datos.duracionHoras() == null
                || datos.pistaId() <= 0
                || datos.duracionHoras() < 1
                || datos.duracionHoras() > 2)
        {
            throw new IllegalArgumentException("Datos de reserva no válidos.");
        }
    }

    private void validarFechaYHorario(CrearReservaPeticion datos, LocalDateTime ahora) {

        LocalDate hoy = ahora.toLocalDate();
        LocalDate fecha = datos.fecha();

        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(13))) {
            throw new IllegalArgumentException("La fecha debe estar entre hoy y los 13 días siguientes");
        }

        LocalDateTime inicio = LocalDateTime.of(fecha, datos.horaInicio());

        if (!inicio.isAfter(ahora)) {
            throw new IllegalArgumentException("La fecha debe ser posterior al momento actual");
        }

        HorarioDia horario = horarios.calcular(fecha);
        if (!horario.cabe(datos.horaInicio(), datos.duracionHoras())) {
            throw new IllegalArgumentException("La reserva debe ocupar horas completas dentro del horario.");
        }
    }

    private SocioRespuesta comprobarSocioActivo(int socioId) {
        SocioRespuesta socio = socios.buscarPorIdParaCambio(socioId).orElseThrow(() -> new NoSuchElementException("Socio no encontrado."));

        if (!"ACTIVO".equals(socio.estado())) {
            throw new ConflictoApiException("SOCIO_INACTIVO", "El socio está inactivo.");
        }

        return socio;
    }

    /** Valida y crea una reserva junto con todas sus horas ocupadas. */
    @Transactional
    public ReservaRespuesta crear(CrearReservaPeticion datos, int socioId, int creadorCuentaId) {
        validarDatos(datos);

        comprobarSocioActivo(socioId);

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        validarFechaYHorario(datos, ahora);

        if (reservas.contarFuturasConfirmadas(socioId, ahora) >= 2) {
            throw new ConflictoApiException("LIMITE_RESERVAS", "El socio ya tiene dos reservas futuras confirmadas.");
        }

        LocalTime fin = datos.horaInicio().plusHours(datos.duracionHoras());

        if (!reservas.existePista(datos.pistaId())) {
            throw new NoSuchElementException("Pista no encontrada.");
        }

        if (reservas.existeSolapeSocio(socioId, datos.fecha(), datos.horaInicio(), fin)) {
            throw new ConflictoApiException(
                    "RESERVA_SOLAPADA",
                    "El socio ya tiene una reserva en alguna de esas horas."
            );
        }

        if (reservas.existeOcupacionPista(datos.pistaId(), datos.fecha(), datos.horaInicio(), fin)) {
            throw new ConflictoApiException(
                    "FRANJA_OCUPADA",
                    "Alguna hora de la pista ya está ocupada."
            );
        }

        int reservaId = reservas.insertarReserva(datos, socioId, creadorCuentaId);

        for (int indice = 0; indice < datos.duracionHoras(); indice++) {
            LocalTime hora = datos.horaInicio().plusHours(indice);

            reservas.insertarOcupacion(
                    datos.pistaId(),
                    datos.fecha(),
                    hora,
                    reservaId
            );
        }

        ReservaRepository.ReservaDatos guardada = reservas.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalStateException("No se encontró la reserva recién creada."));

        return prepararRespuesta(guardada, true);
    }

    /** Convierte los datos almacenados al formato que reciben los clientes. */
    private ReservaRespuesta prepararRespuesta(ReservaRepository.ReservaDatos reserva, boolean puedeCancelar) {
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        return new ReservaRespuesta(
                reserva.id(),
                reserva.socioId(),
                reserva.socioNombre(),
                reserva.pistaId(),
                reserva.pistaNombre(),
                reserva.fecha(),
                reserva.horaInicio().format(formatoHora),
                reserva.horaInicio().plusHours(reserva.duracionHoras()).format(formatoHora),
                reserva.duracionHoras(),
                reserva.estado(),
                reserva.creadaEn(),
                reserva.canceladaEn(),
                reserva.canceladaPor(),
                reserva.motivoCancelacion(),
                puedeCancelar
        );
    }
}

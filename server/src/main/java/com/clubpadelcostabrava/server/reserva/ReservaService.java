package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.api.ConflictoApiException;
import com.clubpadelcostabrava.server.calendario.HorarioDia;
import com.clubpadelcostabrava.server.calendario.HorarioService;
import com.clubpadelcostabrava.server.socio.SocioRepository;
import com.clubpadelcostabrava.server.socio.SocioRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final SocioRepository socios;
    private final HorarioService horarios;
    private static final int TAMANO_PAGINA = 50;

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

    /** Indica si el socio aún puede cancelar esta reserva. */
    boolean puedeCancelarSocio(ReservaRepository.ReservaDatos reserva, ZonedDateTime ahora) {

        if (!"CONFIRMADA".equals(reserva.estado())) {
            return false;
        }

        ZonedDateTime inicio = ZonedDateTime.of(reserva.fecha(), reserva.horaInicio(), ZoneId.of("Europe/Madrid"));

        if (!inicio.isAfter(ahora)) {
            return false;
        }

        if (reserva.fecha().isAfter(ahora.toLocalDate())) {
            return true;
        }

        return !ahora.toInstant().isAfter(reserva.creadaEn().toInstant().plus(Duration.ofMinutes(10)));
    }

    /** Indica si administración puede cancelar una reserva futura o en curso. */
    boolean puedeCancelarAdmin(
            ReservaRepository.ReservaDatos reserva,
            ZonedDateTime ahora
    ) {
        if (!"CONFIRMADA".equals(reserva.estado())) {
            return false;
        }

        ZonedDateTime inicio = ZonedDateTime.of(
                reserva.fecha(),
                reserva.horaInicio(),
                ZoneId.of("Europe/Madrid")
        );
        ZonedDateTime fin = inicio.plusHours(reserva.duracionHoras());

        return fin.isAfter(ahora);
    }

    /** Cancela una reserva propia y libera todas sus horas en una transacción. */
    @Transactional
    public ReservaRespuesta cancelarPropia(
            int reservaId, int socioId, int cuentaId
    ) {
        ReservaRepository.ReservaDatos reserva =
                reservas.buscarPorIdParaCancelar(reservaId)
                        .orElseThrow(() ->
                                new NoSuchElementException("Reserva no encontrada."));

        if (reserva.socioId() != socioId) {
            throw new NoSuchElementException("Reserva no encontrada.");
        }

        if (!"CONFIRMADA".equals(reserva.estado())) {
            throw new ConflictoApiException(
                    "RESERVA_YA_CANCELADA",
                    "La reserva ya está cancelada."
            );
        }

        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));

        if (!puedeCancelarSocio(reserva, ahora)) {
            throw new ConflictoApiException(
                    "PLAZO_CANCELACION",
                    "Ya no puedes cancelar esta reserva."
            );
        }

        return guardarCancelacion(reserva, cuentaId, null);
    }

    /** Guarda una cancelación ya autorizada y libera todas sus horas. */
    private ReservaRespuesta guardarCancelacion(
            ReservaRepository.ReservaDatos reserva,
            int cuentaId,
            String motivo
    ) {
        int reservaId = reserva.id();

        int filasActualizadas =
                reservas.marcarCancelada(reservaId, cuentaId, motivo);
        if (filasActualizadas != 1) {
            throw new IllegalStateException("No se pudo actualizar la reserva.");
        }

        int horasLiberadas = reservas.eliminarOcupaciones(reservaId);
        if (horasLiberadas != reserva.duracionHoras()) {
            throw new IllegalStateException(
                    "No se liberaron todas las horas de la reserva."
            );
        }

        ReservaRepository.ReservaDatos cancelada =
                reservas.buscarPorId(reservaId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encontró la reserva cancelada."
                                ));

        return prepararRespuesta(cancelada, false);
    }

    /** Exige un motivo visible para el socio y compatible con la base de datos. */
    private String validarMotivoAdmin(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio.");
        }

        String limpio = motivo.strip();

        if (limpio.codePointCount(0, limpio.length()) > 200) {
            throw new IllegalArgumentException(
                    "El motivo no puede superar los 200 caracteres."
            );
        }

        return limpio;
    }

    /** Comprueba si ADMIN puede cancelar una reserva futura o en curso. */
    @Transactional
    public ResultadoCancelacionAdmin cancelarAdmin(int reservaId, int adminCuentaId, String motivo) {

        String motivoLimpio = validarMotivoAdmin(motivo);

        ReservaRepository.ReservaDatos reserva =
                reservas.buscarPorIdParaCancelar(reservaId)
                        .orElseThrow(() ->
                                new NoSuchElementException("Reserva no encontrada."));

        if (!"CONFIRMADA".equals(reserva.estado())) {
            throw new ConflictoApiException(
                    "RESERVA_YA_CANCELADA",
                    "La reserva ya está cancelada."
            );
        }

        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));

        if (!puedeCancelarAdmin(reserva, ahora)) {
            throw new ConflictoApiException(
                    "RESERVA_TERMINADA",
                    "No se puede cancelar una reserva terminada."
            );
        }

        String correo = socios.buscarPorId(reserva.socioId())
                .orElseThrow(() ->
                        new IllegalStateException("No se encontró el correo del socio."))
                .correo();

        ReservaRespuesta cancelada =
                guardarCancelacion(reserva, adminCuentaId, motivoLimpio);

        return new ResultadoCancelacionAdmin(cancelada, correo);
    }

    /** Devuelve las reservas del socio con el permiso de cancelación actualizado. */
    public List<ReservaRespuesta> listarPorSocio(int socioId) {
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        List<ReservaRespuesta> respuesta = new ArrayList<>();

        for (ReservaRepository.ReservaDatos reserva : reservas.listarPorSocio(socioId)) {
            boolean puedeCancelar = puedeCancelarSocio(reserva, ahora);
            respuesta.add(prepararRespuesta(reserva, puedeCancelar));
        }

        return List.copyOf(respuesta);
    }

    /** Devuelve las reservas filtradas con el permiso de cancelación de ADMIN. */
    public List<ReservaRespuesta> listarParaAdmin(
            LocalDate fecha, Integer pistaId, Integer socioId
    ) {
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        List<ReservaRespuesta> respuesta = new ArrayList<>();

        for (ReservaRepository.ReservaDatos reserva :
                reservas.listarParaAdmin(fecha, pistaId, socioId)) {
            boolean puedeCancelar = puedeCancelarAdmin(reserva, ahora);
            respuesta.add(prepararRespuesta(reserva, puedeCancelar));
        }

        return List.copyOf(respuesta);
    }

    /** Devuelve una página de reservas actuales o de la fecha indicada. */
    public PaginaReservasRespuesta listarPaginaParaAdmin(LocalDate fecha, int pagina) {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa.");
        }

        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        LocalDate desde = fecha == null ? ahora.toLocalDate() : null;

        List<ReservaRepository.ReservaDatos> filas = reservas.listarParaAdmin(
                fecha,
                null,
                null,
                desde,
                TAMANO_PAGINA + 1,
                pagina * (long) TAMANO_PAGINA
        );

        boolean hayMas = filas.size() > TAMANO_PAGINA;
        List<ReservaRespuesta> respuesta = new ArrayList<>();

        int visibles = Math.min(TAMANO_PAGINA, filas.size());
        for (int i = 0; i < visibles; i++) {
            ReservaRepository.ReservaDatos reserva = filas.get(i);
            boolean puedeCancelar = puedeCancelarAdmin(reserva, ahora);
            respuesta.add(prepararRespuesta(reserva, puedeCancelar));
        }

        return new PaginaReservasRespuesta(List.copyOf(respuesta), hayMas);
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

    /** Datos necesarios para enviar el aviso después de guardar la cancelación. */
    public record ResultadoCancelacionAdmin(ReservaRespuesta reserva, String correo) {}
}

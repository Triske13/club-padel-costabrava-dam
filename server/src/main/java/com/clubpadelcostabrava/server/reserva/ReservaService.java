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

/**
 * Aplica las reglas de negocio para crear, consultar y cancelar reservas.
 * Coordina los repositorios de reservas y socios y el horario del club.
 */
@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final SocioRepository socios;
    private final HorarioService horarios;
    private static final int TAMANO_PAGINA = 50;

    /**
     * Recibe los componentes necesarios para consultar reservas y socios
     * y calcular el horario aplicable.
     *
     * @param reservas acceso a reservas y ocupaciones
     * @param socios   acceso a las fichas de socios
     * @param horarios cálculo del horario del club
     */
    public ReservaService(ReservaRepository reservas, SocioRepository socios, HorarioService horarios) {
        this.reservas = reservas;
        this.socios = socios;
        this.horarios = horarios;
    }

    /**
     * Comprueba que la petición tenga los datos obligatorios, una pista válida
     * y una duración de una o dos horas.
     *
     * @param datos petición que se va a validar
     * @throws IllegalArgumentException si falta un dato o su valor no es válido
     */
    private void validarDatos(CrearReservaPeticion datos) {

        if (datos == null
                || datos.pistaId() == null
                || datos.fecha() == null
                || datos.horaInicio() == null
                || datos.duracionHoras() == null
                || datos.pistaId() <= 0
                || datos.duracionHoras() < 1
                || datos.duracionHoras() > 2) {
            throw new IllegalArgumentException("Datos de reserva no válidos.");
        }
    }

    /**
     * Comprueba el plazo de reserva, que el inicio sea futuro y que todas
     * las horas solicitadas quepan en el horario de apertura.
     *
     * @param datos petición previamente validada
     * @param ahora fecha y hora de referencia en Europe/Madrid
     * @throws IllegalArgumentException si la fecha o el horario no son válidos
     */
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

    /**
     * Busca al socio y exige que su estado sea ACTIVO.
     *
     * @param socioId identificador del socio
     * @return ficha del socio activo
     * @throws NoSuchElementException si el socio no existe
     * @throws ConflictoApiException  si el socio está inactivo
     */
    private SocioRespuesta comprobarSocioActivo(int socioId) {
        SocioRespuesta socio = socios.buscarPorIdParaCambio(socioId).orElseThrow(() -> new NoSuchElementException("Socio no encontrado."));

        if (!"ACTIVO".equals(socio.estado())) {
            throw new ConflictoApiException("SOCIO_INACTIVO", "El socio está inactivo.");
        }

        return socio;
    }

    /**
     * Valida y crea una reserva confirmada junto con sus ocupaciones.
     * La operación es transaccional: si falla alguna hora, se deshacen todos los cambios.
     *
     * @param datos           pista, fecha, hora de inicio y duración solicitadas
     * @param socioId         identificador del socio titular
     * @param creadorCuentaId identificador de la cuenta que realiza la operación
     * @return la reserva creada con los datos que reciben los clientes
     * @throws IllegalArgumentException si los datos, la fecha o el horario no son válidos
     * @throws NoSuchElementException   si no existe el socio o la pista
     * @throws ConflictoApiException    si se supera el cupo o existe un solape u ocupación
     */
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

    /**
     * Evalúa si el socio puede cancelar una reserva confirmada.
     * Hasta el día anterior puede cancelarla antes de su inicio; si empieza
     * hoy, debe hacerlo dentro de los diez minutos desde su creación
     * y siempre antes del inicio.
     *
     * @param reserva reserva que se comprueba
     * @param ahora   fecha y hora de referencia
     * @return true si el socio todavía puede cancelarla
     */
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

    /**
     * Comprueba si administración puede cancelar una reserva confirmada.
     * La cancelación se permite mientras no haya terminado.
     *
     * @param reserva reserva que se comprueba
     * @param ahora   fecha y hora de referencia
     * @return true si la reserva sigue en curso o aún no ha comenzado
     */
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

    /**
     * Cancela una reserva del socio y libera todas sus horas en una transacción.
     * Solo permite cancelar reservas propias dentro del plazo establecido.
     *
     * @param reservaId identificador de la reserva
     * @param socioId   identificador del socio que la tiene asignada
     * @param cuentaId  identificador de la cuenta que solicita la cancelación
     * @return la reserva actualizada en estado CANCELADA
     * @throws NoSuchElementException si la reserva no existe o no pertenece al socio
     * @throws ConflictoApiException  si ya está cancelada o ha vencido el plazo
     */
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

    /**
     * Guarda una cancelación ya autorizada y elimina todas sus ocupaciones.
     * Lo llaman los métodos transaccionales de cancelación.
     *
     * @param reserva  reserva que se va a cancelar
     * @param cuentaId cuenta que realiza la cancelación
     * @param motivo   motivo administrativo, o null si cancela el socio
     * @return reserva actualizada para la respuesta
     * @throws IllegalStateException si no se actualiza la reserva o no se liberan todas sus horas
     */
    private ReservaRespuesta guardarCancelacion(ReservaRepository.ReservaDatos reserva, int cuentaId, String motivo) {
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

    /**
     * Limpia y valida el motivo de una cancelación administrativa.
     *
     * @param motivo texto introducido por administración
     * @return motivo sin espacios al principio ni al final
     * @throws IllegalArgumentException si está vacío o supera los 200 caracteres
     */
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

    /**
     * Cancela una reserva futura o en curso por decisión de administración.
     * Guarda el motivo y libera las ocupaciones en una transacción.
     * Devuelve el correo del socio para intentar el aviso después.
     *
     * @param reservaId     identificador de la reserva
     * @param adminCuentaId identificador de la cuenta administradora
     * @param motivo        explicación de la cancelación que verá el socio
     * @return la reserva cancelada y el correo al que se intentará enviar el aviso
     * @throws IllegalArgumentException si falta el motivo o supera el límite permitido
     * @throws NoSuchElementException   si la reserva no existe
     * @throws ConflictoApiException    si ya está cancelada o ha terminado
     */
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

    /**
     * Consulta todas las reservas de un socio, incluidas las canceladas.
     * Calcula para cada una si el socio aún puede cancelarla.
     *
     * @param socioId identificador del socio
     * @return reservas del socio con el permiso de cancelación actualizado
     */
    public List<ReservaRespuesta> listarPorSocio(int socioId) {
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        List<ReservaRespuesta> respuesta = new ArrayList<>();

        for (ReservaRepository.ReservaDatos reserva : reservas.listarPorSocio(socioId)) {
            boolean puedeCancelar = puedeCancelarSocio(reserva, ahora);
            respuesta.add(prepararRespuesta(reserva, puedeCancelar));
        }

        return List.copyOf(respuesta);
    }

    /**
     * Consulta las reservas que coinciden con los filtros indicados.
     * Un filtro nulo no restringe ese campo.
     *
     * @param fecha   fecha por la que filtrar, o null para cualquier fecha
     * @param pistaId pista por la que filtrar, o null para cualquier pista
     * @param socioId socio por el que filtrar, o null para cualquier socio
     * @return reservas encontradas con el permiso de cancelación de ADMIN actualizado
     */
    public List<ReservaRespuesta> listarParaAdmin(LocalDate fecha, Integer pistaId, Integer socioId) {
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        List<ReservaRespuesta> respuesta = new ArrayList<>();

        for (ReservaRepository.ReservaDatos reserva :
                reservas.listarParaAdmin(fecha, pistaId, socioId)) {
            boolean puedeCancelar = puedeCancelarAdmin(reserva, ahora);
            respuesta.add(prepararRespuesta(reserva, puedeCancelar));
        }

        return List.copyOf(respuesta);
    }

    /**
     * Consulta una página de 50 reservas para administración.
     * Sin fecha muestra reservas desde hoy; con fecha, solo las de ese día.
     *
     * @param fecha  fecha concreta, o null para consultar desde hoy
     * @param pagina número de página empezando por cero
     * @return reservas de la página e indicador de si existen más resultados
     * @throws IllegalArgumentException si la página es negativa
     */
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

    /**
     * Convierte los datos almacenados en la respuesta enviada a los clientes.
     * Formatea las horas como HH:mm e incluye el permiso de cancelación calculado.
     *
     * @param reserva       datos de la reserva obtenidos del repositorio
     * @param puedeCancelar indica si el usuario de esta consulta puede cancelarla
     * @return respuesta de reserva para la API
     */
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

    /**
     * Reúne la reserva cancelada y el correo necesario para intentar
     * el aviso después de completar la transacción.
     *
     * @param reserva reserva ya cancelada
     * @param correo  dirección del socio al que se intentará avisar
     */
    public record ResultadoCancelacionAdmin(ReservaRespuesta reserva, String correo) {
    }
}

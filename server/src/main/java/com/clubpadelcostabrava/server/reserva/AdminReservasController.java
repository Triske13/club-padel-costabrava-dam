package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.api.ConflictoApiException;
import com.clubpadelcostabrava.server.api.ErrorApi;
import com.clubpadelcostabrava.server.correo.AvisoCancelacionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/**
 * Expone a administración la creación, consulta y cancelación de reservas.
 * El acceso a estas rutas requiere el rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservasController {

    private final ReservaService servicio;
    private final AvisoCancelacionService avisos;

    /**
     * Recibe el servicio de reservas y el encargado de enviar avisos.
     *
     * @param servicio operaciones de reserva
     * @param avisos   envío de avisos de cancelación
     */
    public AdminReservasController(ReservaService servicio, AvisoCancelacionService avisos) {
        this.servicio = servicio;
        this.avisos = avisos;
    }

    /**
     * Crea una reserva para el socio indicado por administración.
     * Aplica las mismas reglas de reserva que la ruta del socio.
     *
     * @param datos          socio titular y datos de la reserva solicitada
     * @param authentication sesión de la cuenta administradora
     * @return 201 con la reserva creada, o un error HTTP si no puede crearse
     */
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody(required = false) CrearReservaAdminPeticion datos,
            Authentication authentication
    ) {
        if (datos == null || datos.socioId() == null || datos.socioId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Indica un socio válido."));
        }

        int adminCuentaId = (Integer) authentication.getPrincipal();

        CrearReservaPeticion peticion = new CrearReservaPeticion(datos.pistaId(), datos.fecha(), datos.horaInicio(), datos.duracionHoras());

        try {
            ReservaRespuesta creada = servicio.crear(peticion, datos.socioId(), adminCuentaId);

            return ResponseEntity.status(HttpStatus.CREATED).body(creada);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));

        } catch (NoSuchElementException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", e.getMessage()));

        } catch (ConflictoApiException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi(e.codigo(), e.getMessage()));

        } catch (DuplicateKeyException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi("FRANJA_OCUPADA", "Alguna hora de la pista ya está ocupada."));
        }
    }

    /**
     * Consulta reservas mediante filtros opcionales, sin paginación.
     *
     * @param fechaTexto   fecha en formato ISO, o null
     * @param pistaIdTexto identificador de pista, o null
     * @param socioIdTexto identificador de socio, o null
     * @return 200 con las reservas encontradas, o 400 si un filtro no es válido
     */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(value = "fecha", required = false) String fechaTexto,
            @RequestParam(value = "pistaId", required = false) String pistaIdTexto,
            @RequestParam(value = "socioId", required = false) String socioIdTexto
    ) {
        LocalDate fecha;
        Integer pistaId;
        Integer socioId;

        try {
            fecha = fechaTexto == null ? null : LocalDate.parse(fechaTexto);
            pistaId = pistaIdTexto == null ? null : Integer.valueOf(pistaIdTexto);
            socioId = socioIdTexto == null ? null : Integer.valueOf(socioIdTexto);
        } catch (DateTimeParseException | NumberFormatException e) {

            return ResponseEntity.badRequest().body(new ErrorApi("DATOS_INVALIDOS", "Filtros de reserva no válidos."));
        }

        if ((pistaId != null && pistaId <= 0)
                || (socioId != null && socioId <= 0)) {

            return ResponseEntity.badRequest().body(new ErrorApi("DATOS_INVALIDOS", "Los identificadores deben ser positivos."));
        }

        return ResponseEntity.ok(servicio.listarParaAdmin(fecha, pistaId, socioId));
    }

    /**
     * Consulta reservas en páginas de 50 elementos.
     * Sin fecha muestra las reservas desde hoy; con fecha consulta
     * el histórico de ese día, que debe ser anterior a hoy.
     *
     * @param fechaTexto  fecha histórica en formato ISO, o null para reservas desde hoy
     * @param paginaTexto número de página empezando por cero
     * @return 200 con la página y el indicador hayMas, o 400 si los datos no son válidos
     */
    @GetMapping("/pagina")
    public ResponseEntity<?> listarPagina(
            @RequestParam(value = "fecha", required = false) String fechaTexto,
            @RequestParam(value = "pagina", defaultValue = "0") String paginaTexto
    ) {
        LocalDate fecha;
        int pagina;

        try {
            fecha = fechaTexto == null ? null : LocalDate.parse(fechaTexto);
            pagina = Integer.parseInt(paginaTexto);
        } catch (DateTimeParseException | NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Fecha o página no válida."));
        }

        if (pagina < 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "La página no puede ser negativa."));
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        if (fecha != null && !fecha.isBefore(hoy)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "DATOS_INVALIDOS",
                            "Selecciona una fecha anterior a hoy para el histórico."
                    ));
        }

        return ResponseEntity.ok(servicio.listarPaginaParaAdmin(fecha, pagina));
    }

    /**
     * Cancela una reserva con motivo obligatorio y después intenta avisar al socio.
     * El resultado del correo se informa sin deshacer la cancelación.
     *
     * @param id             identificador de la reserva
     * @param datos          motivo de la cancelación
     * @param authentication sesión de la cuenta administradora
     * @return 200 con la reserva y el resultado del aviso, o un error HTTP
     */
    @PostMapping("/{id}/cancelacion")
    public ResponseEntity<?> cancelar(
            @PathVariable int id,
            @RequestBody(required = false) CancelarReservaAdminPeticion datos,
            Authentication authentication
    ) {
        int adminCuentaId = (Integer) authentication.getPrincipal();

        try {
            ReservaService.ResultadoCancelacionAdmin resultado =
                    servicio.cancelarAdmin(
                            id,
                            adminCuentaId,
                            datos == null ? null : datos.motivo()
                    );

            boolean correoAceptado = avisos.avisarCancelacionAdmin(
                    resultado.correo(),
                    resultado.reserva()
            );

            return ResponseEntity.ok(new CancelarReservaAdminRespuesta(
                    resultado.reserva(),
                    correoAceptado ? "ACEPTADO" : "FALLIDO"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", e.getMessage()));

        } catch (ConflictoApiException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi(e.codigo(), e.getMessage()));
        }
    }
}

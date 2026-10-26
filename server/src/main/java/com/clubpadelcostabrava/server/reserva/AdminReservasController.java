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
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/** Atiende las operaciones de reservas realizadas por administración. */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservasController {

    private final ReservaService servicio;
    private final AvisoCancelacionService avisos;

    public AdminReservasController(ReservaService servicio, AvisoCancelacionService avisos) {
        this.servicio = servicio;
        this.avisos = avisos;
    }

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

    /** Lista reservas con los filtros opcionales de administración. */
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

    /** Cancela una reserva y, una vez guardada, intenta avisar al socio. */
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

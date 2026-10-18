package com.clubpadelcostabrava.server.bloqueo;

import com.clubpadelcostabrava.server.api.ErrorApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/** Expone a administración las operaciones HTTP para gestionar bloqueos. */
@RestController
@RequestMapping("/api/admin/bloqueos")
public class BloqueoController {

    private final BloqueoService servicio;

    public BloqueoController(BloqueoService servicio) {
        this.servicio = servicio;
    }

    /** Consulta los bloqueos de la fecha indicada en la URL. */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(value = "fecha", required = false) String fecha
    ) {
        try {
            LocalDate dia = fecha == null ? null : LocalDate.parse(fecha);

            return ResponseEntity.ok(servicio.listarPorFecha(dia));

        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "DATOS_INVALIDOS",
                            "Indica una fecha válida con formato AAAA-MM-DD."
                    ));
        }
    }

    /** Crea un bloqueo usando la identidad del administrador autenticado. */
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody(required = false) CrearBloqueoPeticion datos,
            Authentication authentication
    ) {
        try {
            int adminId = (Integer) authentication.getPrincipal();
            BloqueoRespuesta creado = servicio.crear(datos, adminId);

            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", e.getMessage()));

        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi(
                            "FRANJA_OCUPADA",
                            "Alguna hora de la pista ya está ocupada."
                    ));
        }
    }

    /** Retira un bloqueo futuro y responde sin contenido si se elimina. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> retirar(@PathVariable("id") int id) {
        try {
            servicio.retirar(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi(
                            "CANCELACION_NO_PERMITIDA",
                            e.getMessage()
                    ));
        }
    }
}
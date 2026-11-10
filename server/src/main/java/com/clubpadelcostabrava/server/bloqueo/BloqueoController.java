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

/**
 * Expone a administración las rutas HTTP para consultar,
 * crear y retirar bloqueos de pista.
 */
@RestController
@RequestMapping("/api/admin/bloqueos")
public class BloqueoController {

    private final BloqueoService servicio;

    /**
     * Recibe el servicio que aplica las reglas de los bloqueos.
     *
     * @param servicio operaciones de bloqueo
     */
    public BloqueoController(BloqueoService servicio) {
        this.servicio = servicio;
    }

    /**
     * Consulta los bloqueos de la fecha indicada.
     *
     * @param fecha fecha en formato AAAA-MM-DD
     * @return 200 con los bloqueos del día, o 400 si falta o no es válida
     */
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

    /**
     * Crea un bloqueo a nombre de la cuenta administradora autenticada.
     *
     * @param datos          pista, fecha, hora, duración y descripción solicitadas
     * @param authentication sesión de la cuenta administradora
     * @return 201 con el bloqueo creado, o un error HTTP si no puede crearse
     */
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

    /**
     * Retira un bloqueo que aún no ha comenzado.
     *
     * @param id identificador del bloqueo
     * @return 204 si se retiró, 404 si no existe
     * o 409 si ya había comenzado
     */
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
package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.api.ConflictoApiException;
import com.clubpadelcostabrava.server.api.ErrorApi;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/** Atiende las operaciones de reservas realizadas por administración. */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservasController {

    private final ReservaService servicio;

    public AdminReservasController(ReservaService servicio) {
        this.servicio = servicio;
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
}

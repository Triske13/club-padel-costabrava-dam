package com.clubpadelcostabrava.server.reserva;

import com.clubpadelcostabrava.server.api.ConflictoApiException;
import com.clubpadelcostabrava.server.api.ErrorApi;
import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/mis-reservas")
public class MisReservasController {
    private final ReservaService servicio;
    private final CuentaRepository cuentas;

    public MisReservasController(ReservaService servicio, CuentaRepository cuentas) {
        this.servicio = servicio;
        this.cuentas = cuentas;
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody(required = false) CrearReservaPeticion datos,
            Authentication authentication
    ) {
        int cuentaId = (Integer) authentication.getPrincipal();
        Integer socioId = cuentas.socioIdPorCuenta(cuentaId).orElse(null);

        if (socioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi(
                            "SESION_INVALIDA",
                            "Debes iniciar sesión de nuevo."
                    ));
        }

        try {
            ReservaRespuesta creada = servicio.crear(datos, socioId, cuentaId);
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
                    .body(new ErrorApi(
                            "FRANJA_OCUPADA",
                            "Alguna hora de la pista ya está ocupada."
                    ));
        }
    }

    /** Lista únicamente las reservas del socio asociado a la sesión. */
    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        int cuentaId = (Integer) authentication.getPrincipal();
        Integer socioId = cuentas.socioIdPorCuenta(cuentaId).orElse(null);

        if (socioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi(
                            "SESION_INVALIDA",
                            "Debes iniciar sesión de nuevo."
                    ));
        }

        return ResponseEntity.ok(servicio.listarPorSocio(socioId));
    }
}

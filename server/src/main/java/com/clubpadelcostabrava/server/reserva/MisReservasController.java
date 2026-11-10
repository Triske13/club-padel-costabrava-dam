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

/**
 * Expone las operaciones de consulta, creación y cancelación
 * de las reservas del socio autenticado.
 */
@RestController
@RequestMapping("/api/mis-reservas")
public class MisReservasController {
    private final ReservaService servicio;
    private final CuentaRepository cuentas;

    /**
     * Recibe el servicio de reservas y el repositorio que relaciona
     * la cuenta autenticada con su ficha de socio.
     *
     * @param servicio operaciones de reserva
     * @param cuentas  acceso a las cuentas
     */
    public MisReservasController(ReservaService servicio, CuentaRepository cuentas) {
        this.servicio = servicio;
        this.cuentas = cuentas;
    }

    /**
     * Crea una reserva para el socio asociado a la cuenta autenticada.
     *
     * @param datos          pista, fecha, hora y duración solicitadas
     * @param authentication sesión de la que se obtiene la cuenta del socio
     * @return 201 con la reserva creada, o un error HTTP si no puede crearse
     */
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

    /**
     * Lista las reservas del socio asociado a la cuenta autenticada,
     * incluidas las canceladas.
     *
     * @param authentication sesión de la que se obtiene la cuenta del socio
     * @return 200 con las reservas, o 401 si la cuenta no tiene socio asociado
     */
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

    /**
     * Cancela una reserva propia si aún cumple las reglas de plazo.
     *
     * @param id             identificador de la reserva
     * @param authentication sesión de la que se obtiene el socio titular
     * @return 200 con la reserva cancelada, 404 si no es propia
     * o 409 si ya no puede cancelarse
     */
    @PostMapping("/{id}/cancelacion")
    public ResponseEntity<?> cancelar(
            @PathVariable int id,
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
            ReservaRespuesta cancelada =
                    servicio.cancelarPropia(id, socioId, cuentaId);
            return ResponseEntity.ok(cancelada);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", e.getMessage()));

        } catch (ConflictoApiException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi(e.codigo(), e.getMessage()));
        }
    }
}

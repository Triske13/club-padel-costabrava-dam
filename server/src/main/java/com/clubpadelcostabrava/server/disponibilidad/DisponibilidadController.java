package com.clubpadelcostabrava.server.disponibilidad;

import com.clubpadelcostabrava.server.api.ErrorApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Expone el detalle diario de disponibilidad para USER y ADMIN. */
@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService servicio;

    public DisponibilidadController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    /** Interpreta la fecha y delega en el servicio las reglas de consulta. */
    @GetMapping
    public ResponseEntity<?> consultar(
            @RequestParam(value = "fecha", required = false) String fecha,
            Authentication authentication
    ) {
        try {
            LocalDate dia = fecha == null ? null : LocalDate.parse(fecha);

            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(autoridad ->
                            "ROLE_ADMIN".equals(autoridad.getAuthority()));

            return ResponseEntity.ok(servicio.consultarDia(dia, esAdmin));

        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "DATOS_INVALIDOS",
                            "Indica una fecha válida dentro del plazo de consulta."
                    ));
        }
    }
}

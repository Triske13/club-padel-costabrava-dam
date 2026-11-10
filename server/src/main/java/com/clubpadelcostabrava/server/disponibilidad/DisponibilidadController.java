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

/**
 * Expone el detalle diario de disponibilidad para USER y ADMIN.
 * El servicio aplica el plazo de consulta correspondiente a cada rol.
 */
@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService servicio;

    /**
     * Recibe el servicio que calcula la disponibilidad.
     *
     * @param servicio cálculo de disponibilidad diaria
     */
    public DisponibilidadController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    /**
     * Consulta el horario y las franjas de todas las pistas para una fecha.
     *
     * @param fecha          fecha en formato AAAA-MM-DD
     * @param authentication sesión utilizada para determinar si el usuario es ADMIN
     * @return 200 con la disponibilidad del día, o 400 si la fecha no es válida
     */
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

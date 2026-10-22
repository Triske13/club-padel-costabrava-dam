package com.clubpadelcostabrava.server.disponibilidad;

import com.clubpadelcostabrava.server.api.ErrorApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Expone el resumen mensual utilizado por el calendario. */
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final DisponibilidadService servicio;

    public CalendarioController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    /** Interpreta el mes y obtiene los indicadores de todos sus días. */
    @GetMapping
    public ResponseEntity<?> consultar(
            @RequestParam(value = "mes", required = false) String mes,
            Authentication authentication
    ) {
        try {
            YearMonth mesSolicitado = mes == null ? null : YearMonth.parse(mes);

            boolean esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(autoridad ->
                            "ROLE_ADMIN".equals(autoridad.getAuthority()));

            return ResponseEntity.ok(
                    servicio.consultarMes(mesSolicitado, esAdmin)
            );

        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "DATOS_INVALIDOS",
                            "Indica un mes válido con formato AAAA-MM."
                    ));
        }
    }
}

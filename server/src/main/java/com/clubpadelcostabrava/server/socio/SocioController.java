package com.clubpadelcostabrava.server.socio;

import com.clubpadelcostabrava.server.api.CredencialesInvalidasException;
import com.clubpadelcostabrava.server.api.ErrorApi;
import com.clubpadelcostabrava.server.auth.SesionService;
import com.clubpadelcostabrava.server.correo.AvisoCancelacionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Expone a los administradores las operaciones HTTP de gestión de socios. */
@RestController
@RequestMapping("/api/admin/socios")
public class SocioController {
    private final SocioRepository socios;
    private final SocioService servicio;
    private final SesionService sesiones;
    private final AvisoCancelacionService avisos;

    public SocioController(SocioRepository socios, SocioService servicio, SesionService sesiones, AvisoCancelacionService avisos) {
        this.socios = socios;
        this.servicio = servicio;
        this.sesiones = sesiones;
        this.avisos = avisos;
    }

    /** Lista todos los socios o filtra por identificador, nombre o correo. */
    @GetMapping
    public List<SocioRespuesta> listar(
            @RequestParam(value = "buscar", required = false) String buscar
    ) {
        return socios.buscar(buscar);
    }

    /** Devuelve la ficha solicitada o un error 404 si no existe. */
    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable("id") int id) {
        Optional<SocioRespuesta> encontrado = socios.buscarPorId(id);

        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", "Socio no encontrado."));
        }

        return ResponseEntity.ok(encontrado.get());
    }

    /** Actualiza los datos personales y traduce los errores de validación a HTTP. */
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(
            @PathVariable("id") int id,
            @RequestBody(required = false) EditarSocioPeticion datos
    ) {
        try {
            Optional<SocioRespuesta> actualizado = servicio.editar(id, datos);

            if (actualizado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorApi("NO_ENCONTRADO", "Socio no encontrado."));
            }

            return ResponseEntity.ok(actualizado.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));
        }
    }

    /** Cambia el correo tras verificar la contraseña del administrador autenticado. */
    @PutMapping("/{id}/correo")
    public ResponseEntity<?> cambiarCorreo(
            @PathVariable("id") int id,
            @RequestBody(required = false) CambiarCorreoPeticion datos,
            Authentication authentication
    ) {
        int adminId = (Integer) authentication.getPrincipal();

        try {
            Optional<SocioRespuesta> actualizado =
                    servicio.cambiarCorreo(id, adminId, datos);

            if (actualizado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorApi("NO_ENCONTRADO", "Socio no encontrado."));
            }

            return ResponseEntity.ok(Map.of("correo", actualizado.get().correo()));
        } catch (CredencialesInvalidasException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi("CREDENCIALES_INVALIDAS", "Contraseña incorrecta."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi("CORREO_EN_USO", "El correo ya pertenece a otra cuenta."));
        }
    }

    /** Crea la ficha y su cuenta USER todavía sin contraseña. */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody(required = false) CrearSocioPeticion datos) {
        try {
            SocioRespuesta creado = servicio.crear(datos);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorApi("CORREO_EN_USO", "El correo ya pertenece a otra cuenta."));
        }
    }

    /** Cambia el estado; después invalida sesiones y avisa por correo si hubo cancelaciones. */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable("id") int id,
            @RequestBody(required = false) CambiarEstadoPeticion datos,
            Authentication authentication
    ) {
        int adminId = (Integer) authentication.getPrincipal();

        try {
            Optional<SocioService.ResultadoEstado> resultado =
                    servicio.cambiarEstado(id, adminId, datos);

            if (resultado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorApi("NO_ENCONTRADO", "Socio no encontrado."));
            }

            SocioService.ResultadoEstado cambio = resultado.get();

            // La invalidación de sesiones se aplica tras completar el cambio en la base de datos.
            if (cambio.cambiado()) {
                sesiones.invalidarCuenta(cambio.cuentaId());
            }

            // El fallo del correo no revierte las reservas ya canceladas.
            String avisoCorreo = "NO_APLICA";
            if (!cambio.reservasCanceladas().isEmpty()) {
                avisoCorreo = avisos.avisarInactivacion(
                        cambio.correo(), cambio.reservasCanceladas()
                ) ? "ACEPTADO" : "FALLIDO";
            }

            return ResponseEntity.ok(new CambiarEstadoRespuesta(
                    cambio.estado(),
                    cambio.reservasCanceladas().size(),
                    avisoCorreo
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", e.getMessage()));
        }
    }
}

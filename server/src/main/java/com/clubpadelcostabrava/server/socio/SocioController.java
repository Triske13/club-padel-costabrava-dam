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

/**
 * Expone a administración las operaciones HTTP de gestión de socios.
 * El acceso a estas rutas requiere el rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/socios")
public class SocioController {
    private final SocioRepository socios;
    private final SocioService servicio;
    private final SesionService sesiones;
    private final AvisoCancelacionService avisos;

    /**
     * Recibe los componentes de consulta, gestión de socios,
     * sesiones y avisos por correo.
     *
     * @param socios   consultas de fichas de socios
     * @param servicio reglas de gestión de socios
     * @param sesiones gestión de sesiones activas
     * @param avisos   envío de avisos por cancelaciones
     */
    public SocioController(SocioRepository socios, SocioService servicio, SesionService sesiones, AvisoCancelacionService avisos) {
        this.socios = socios;
        this.servicio = servicio;
        this.sesiones = sesiones;
        this.avisos = avisos;
    }

    /**
     * Lista los socios o filtra por identificador, nombre o correo.
     *
     * @param buscar criterio opcional de búsqueda
     * @return fichas que cumplen el criterio
     */
    @GetMapping
    public List<SocioRespuesta> listar(
            @RequestParam(value = "buscar", required = false) String buscar
    ) {
        return socios.buscar(buscar);
    }

    /**
     * Consulta la ficha de un socio por identificador.
     *
     * @param id identificador del socio
     * @return 200 con la ficha, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable("id") int id) {
        Optional<SocioRespuesta> encontrado = socios.buscarPorId(id);

        if (encontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorApi("NO_ENCONTRADO", "Socio no encontrado."));
        }

        return ResponseEntity.ok(encontrado.get());
    }

    /**
     * Actualiza los datos personales de un socio sin cambiar su correo.
     *
     * @param id    identificador del socio
     * @param datos nuevos datos personales
     * @return 200 con la ficha actualizada, 404 si no existe
     * o 400 si los datos no son válidos
     */
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

    /**
     * Cambia el correo de un socio tras comprobar la contraseña de ADMIN.
     *
     * @param id             identificador del socio
     * @param datos          nuevo correo y contraseña de confirmación
     * @param authentication sesión de la cuenta administradora
     * @return 200 con el nuevo correo, 401 si la contraseña es incorrecta,
     * 404 si el socio no existe o 409 si el correo ya está en uso
     */
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

    /**
     * Crea la ficha de un socio y su cuenta USER sin contraseña inicial.
     *
     * @param datos datos personales y correo del nuevo socio
     * @return 201 con la ficha creada, 400 si los datos no son válidos
     * o 409 si el correo ya pertenece a otra cuenta
     */
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

    /**
     * Activa o inactiva un socio. Tras guardar el cambio, invalida
     * sus sesiones e intenta avisar por correo si se cancelaron reservas.
     * Un fallo del correo no deshace la inactivación ni las cancelaciones.
     *
     * @param id             identificador del socio
     * @param datos          nuevo estado solicitado
     * @param authentication sesión de la cuenta administradora
     * @return 200 con el estado, el número de reservas canceladas y
     * el resultado del aviso; 404 si el socio no existe
     */
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

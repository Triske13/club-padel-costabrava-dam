package com.clubpadelcostabrava.server.cuenta;

import com.clubpadelcostabrava.server.api.ErrorApi;
import com.clubpadelcostabrava.server.auth.SesionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Atiende las operaciones de la cuenta que ha iniciado sesión. */
@RestController
@RequestMapping("/api/mi-cuenta")
public class MiCuentaController {

    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;

    /** Recibe los componentes necesarios para gestionar la cuenta autenticada. */
    public MiCuentaController(
            CuentaRepository cuentas,
            PasswordEncoder passwordEncoder,
            SesionService sesiones
    ) {
        this.cuentas = cuentas;
        this.passwordEncoder = passwordEncoder;
        this.sesiones = sesiones;
    }

    /** Cambia la contraseña de la cuenta autenticada y cierra todas sus sesiones. */
    @PutMapping("/contrasena")
    public ResponseEntity<?> cambiarContrasena(
            @RequestBody(required = false) CambiarContrasenaPeticion peticion,
            Authentication authentication
    ) {
        if (peticion == null
                || peticion.contrasenaActual() == null
                || peticion.nuevaContrasena() == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Indica ambas contraseñas."));
        }

        String nuevaContrasena = peticion.nuevaContrasena();
        int longitud = nuevaContrasena.codePointCount(0, nuevaContrasena.length());

        if (longitud < 10 || longitud > 64) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "DATOS_INVALIDOS",
                            "La contraseña debe tener entre 10 y 64 caracteres."
                    ));
        }

        int cuentaId = (Integer) authentication.getPrincipal();
        String hashActual = cuentas.buscarHashPorId(cuentaId).orElse(null);

        if (hashActual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi("SESION_INVALIDA", "Debes iniciar sesión de nuevo."));
        }

        if (!passwordEncoder.matches(peticion.contrasenaActual(), hashActual)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi("CREDENCIALES_INVALIDAS", "Contraseña actual incorrecta."));
        }

        String nuevoHash = passwordEncoder.encode(nuevaContrasena);
        cuentas.actualizarContrasenaHash(cuentaId, nuevoHash);
        cuentas.eliminarCodigoPendiente(cuentaId);
        sesiones.invalidarCuenta(cuentaId);

        return ResponseEntity.noContent().build();
    }
}

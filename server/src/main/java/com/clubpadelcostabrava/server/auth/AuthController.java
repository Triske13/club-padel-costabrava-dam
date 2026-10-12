package com.clubpadelcostabrava.server.auth;

import java.time.Instant;
import java.util.Locale;

import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;
    private final LimiteLogin limiteLogin;

    public AuthController(
            CuentaRepository cuentas,
            PasswordEncoder passwordEncoder,
            SesionService sesiones,
            LimiteLogin limiteLogin
    ) {
        this.cuentas = cuentas;
        this.passwordEncoder = passwordEncoder;
        this.sesiones = sesiones;
        this.limiteLogin = limiteLogin;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) LoginPeticion peticion,
            HttpServletRequest request
    ) {
        if (peticion == null || peticion.correo() == null
                || peticion.correo().isBlank() || peticion.contrasena() == null) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Indica correo y contraseña."));
        }

        String correo = peticion.correo().strip().toLowerCase(Locale.ROOT);
        String ip = request.getRemoteAddr();

        if (limiteLogin.estaPausado(correo, ip)) {
            return demasiadosIntentos();
        }

        CuentaRepository.CuentaAcceso cuenta =
                cuentas.buscarPorCorreo(correo).orElse(null);

        String contrasena = peticion.contrasena();
        int longitud = contrasena.codePointCount(0, contrasena.length());

        boolean credencialesValidas = cuenta != null
                && cuenta.contrasenaHash() != null
                && longitud >= 10 && longitud <= 64
                && ("ADMIN".equals(cuenta.rol())
                || ("USER".equals(cuenta.rol())
                && "ACTIVO".equals(cuenta.estadoSocio())))
                && passwordEncoder.matches(contrasena, cuenta.contrasenaHash());

        if (!credencialesValidas) {
            if (limiteLogin.registrarFallo(correo, ip)) {
                return demasiadosIntentos();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorApi(
                            "CREDENCIALES_INVALIDAS",
                            "Correo o contraseña incorrectos."
                    ));
        }

        limiteLogin.reiniciarCorreo(correo);
        SesionService.Sesion sesion = sesiones.crear(cuenta.id(), cuenta.rol());

        return ResponseEntity.ok(new LoginRespuesta(
                sesion.token(),
                sesion.rol(),
                sesion.expiraEn()
        ));
    }

    private ResponseEntity<ErrorApi> demasiadosIntentos() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorApi(
                        "DEMASIADAS_SOLICITUDES",
                        "Espera 15 minutos antes de intentarlo de nuevo."
                ));
    }

    public record LoginPeticion(String correo, String contrasena) {}
    public record LoginRespuesta(String token, String rol, Instant expiraEn) {}
    public record ErrorApi(String codigo, String mensaje) {}
}

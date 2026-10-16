package com.clubpadelcostabrava.server.auth;

import com.clubpadelcostabrava.server.api.ErrorApi;
import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Atiende las peticiones públicas de acceso a la aplicación. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;
    private final LimiteLogin limiteLogin;
    private final LimiteCodigo limiteCodigo;
    private final CodigoTemporalService codigoTemporalService;

    /** Recibe los componentes necesarios para iniciar sesión y gestionar contraseñas. */
    public AuthController(
            CuentaRepository cuentas,
            PasswordEncoder passwordEncoder,
            SesionService sesiones,
            LimiteLogin limiteLogin,
            LimiteCodigo limiteCodigo,
            CodigoTemporalService codigoTemporalService
    ) {
        this.cuentas = cuentas;
        this.passwordEncoder = passwordEncoder;
        this.sesiones = sesiones;
        this.limiteLogin = limiteLogin;
        this.limiteCodigo = limiteCodigo;
        this.codigoTemporalService = codigoTemporalService;
    }

    /** Valida las credenciales, aplica los límites de intentos y devuelve un token si son correctas. */
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

    /**
     * Gestiona la solicitud de un código sin revelar si el correo pertenece
     * a una cuenta que puede recibirlo.
     */
    @PostMapping("/codigo")
    public ResponseEntity<?> codigo(
            @RequestBody(required = false) SolicitarCodigoPeticion peticion,
            HttpServletRequest request
    ) {
        if (peticion == null || peticion.correo() == null
                || peticion.correo().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Indica un correo."));
        }

        String correo = peticion.correo().strip().toLowerCase(Locale.ROOT);
        String ip = request.getRemoteAddr();
        LimiteCodigo.Resultado limite = limiteCodigo.registrarSolicitud(correo, ip);

        if (limite == LimiteCodigo.Resultado.IP_LIMITADA) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorApi("DEMASIADAS_SOLICITUDES", "Superado el límite de solicitudes por IP"));
        }

        if (limite == LimiteCodigo.Resultado.CORREO_LIMITADO) {
            return ResponseEntity.accepted().build();
        }

        CuentaRepository.CuentaAcceso cuenta =
                cuentas.buscarPorCorreo(correo).orElse(null);

        if (cuenta == null) {
            return ResponseEntity.accepted().build();
        }

        if ("USER".equals(cuenta.rol())
                && !"ACTIVO".equals(cuenta.estadoSocio())) {
                    return ResponseEntity.accepted().build();
        }

        codigoTemporalService.solicitarCodigo(cuenta.id(), cuenta.correo());

        return ResponseEntity.accepted().build();
    }

    /**
     * Comprueba el código y, si se cambia la contraseña, invalida las sesiones
     * y elimina la pausa de acceso asociada al correo.
     */
    @PostMapping("/contrasena")
    public ResponseEntity<?> contrasena(
            @RequestBody(required = false) EstablecerContrasenaPeticion peticion
    ) {
        if (peticion == null
                || peticion.correo() == null || peticion.correo().isBlank()
                || peticion.codigo() == null || peticion.codigo().isBlank()
                || peticion.nuevaContrasena() == null){
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "Datos no válidos."));
        }

        if (!peticion.codigo().matches("[0-9]{6}")) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "El código debe tener exactamente 6 dígitos"));
        }

        String nuevaContrasena = peticion.nuevaContrasena();
        int longitud = nuevaContrasena.codePointCount(0, nuevaContrasena.length());

        if (longitud < 10 || longitud > 64) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi("DATOS_INVALIDOS", "La contraseña debe tener entre 10 y 64 caracteres"));
        }

        String correo = peticion.correo().strip().toLowerCase(Locale.ROOT);

        Optional<Integer> cuentaId = codigoTemporalService.establecerContrasena(
                correo,
                peticion.codigo(),
                peticion.nuevaContrasena()
        );

        if (cuentaId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorApi(
                            "CODIGO_INVALIDO",
                            "El código no es válido o ha caducado."
                    ));
        }

        sesiones.invalidarCuenta(cuentaId.get());
        limiteLogin.reiniciarCorreo(correo);

        return ResponseEntity.noContent().build();
    }

    /** Construye la respuesta común cuando una cuenta o IP está temporalmente pausada. */
    private ResponseEntity<ErrorApi> demasiadosIntentos() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorApi(
                        "DEMASIADAS_SOLICITUDES",
                        "Espera 15 minutos antes de intentarlo de nuevo."
                ));
    }

    /** Datos JSON que envía el cliente para iniciar sesión. */
    public record LoginPeticion(String correo, String contrasena) {}
    /** Datos que recibe el cliente después de un inicio de sesión correcto. */
    public record LoginRespuesta(String token, String rol, Instant expiraEn) {}
}

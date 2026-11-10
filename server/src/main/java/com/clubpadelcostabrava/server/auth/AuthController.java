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

/**
 * Expone las operaciones de autenticación, códigos temporales,
 * establecimiento de contraseña y cierre de sesión.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CuentaRepository cuentas;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesiones;
    private final LimiteLogin limiteLogin;
    private final LimiteCodigo limiteCodigo;
    private final CodigoTemporalService codigoTemporalService;

    /**
     * Recibe los componentes necesarios para comprobar cuentas,
     * limitar intentos y gestionar sesiones y códigos.
     *
     * @param cuentas               acceso a las cuentas
     * @param passwordEncoder       comprobación de contraseñas
     * @param sesiones              creación e invalidación de sesiones
     * @param limiteLogin           límite de intentos de acceso
     * @param limiteCodigo          límite de solicitudes de código
     * @param codigoTemporalService generación y comprobación de códigos
     */
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

    /**
     * Comprueba las credenciales y el estado de la cuenta.
     * Aplica límites por correo e IP antes de crear una sesión.
     *
     * @param peticion correo y contraseña enviados por el cliente
     * @param request  petición HTTP de la que se obtiene la IP
     * @return 200 con token, rol y caducidad; 401 si las credenciales
     * no son válidas o 429 si se superó el límite
     */
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
     * Solicita un código temporal para activar o recuperar una cuenta.
     * Responde de forma genérica para no revelar si el correo existe
     * o puede recibir el código. Limita las solicitudes por correo e IP.
     *
     * @param peticion correo al que se solicita el código
     * @param request  petición HTTP de la que se obtiene la IP
     * @return 202 para una solicitud admitida o descartada de forma
     * discreta, 400 si falta el correo o 429 si se limita la IP
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
     * Establece una contraseña mediante un código temporal válido.
     * Después invalida las sesiones de la cuenta y reinicia
     * el límite de intentos de login asociado al correo.
     *
     * @param peticion correo, código y nueva contraseña
     * @return 204 si se estableció la contraseña,
     * o 400 si los datos o el código no son válidos
     */
    @PostMapping("/contrasena")
    public ResponseEntity<?> contrasena(
            @RequestBody(required = false) EstablecerContrasenaPeticion peticion
    ) {
        if (peticion == null
                || peticion.correo() == null || peticion.correo().isBlank()
                || peticion.codigo() == null || peticion.codigo().isBlank()
                || peticion.nuevaContrasena() == null) {
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

    /**
     * Invalida la sesión identificada por el token Bearer de la petición.
     *
     * @param request petición HTTP que contiene la cabecera Authorization
     * @return 204 sin cuerpo tras cerrar la sesión
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        String token = cabecera.substring("Bearer ".length());

        sesiones.invalidar(token);

        return ResponseEntity.noContent().build();
    }

    /**
     * Construye la respuesta común para un acceso temporalmente limitado.
     *
     * @return error HTTP 429 con el tiempo de espera indicado al cliente
     */
    private ResponseEntity<ErrorApi> demasiadosIntentos() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorApi(
                        "DEMASIADAS_SOLICITUDES",
                        "Espera 15 minutos antes de intentarlo de nuevo."
                ));
    }

    /**
     * Credenciales enviadas por el cliente para iniciar sesión.
     *
     * @param correo     dirección de correo de la cuenta
     * @param contrasena contraseña introducida
     */
    public record LoginPeticion(String correo, String contrasena) {
    }

    /**
     * Datos devueltos tras un inicio de sesión correcto.
     *
     * @param token    token Bearer de la sesión
     * @param rol      rol de la cuenta autenticada
     * @param expiraEn instante en que caduca la sesión
     */
    public record LoginRespuesta(String token, String rol, Instant expiraEn) {
    }
}

package com.clubpadelcostabrava.server.auth;

import com.clubpadelcostabrava.server.cuenta.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Genera y envía códigos temporales y comprueba cambios de contraseña con ellos. */
@Service
public class CodigoTemporalService {
    private final CodigoTemporalRepository codigoTemporal;
    private final PasswordEncoder passwordEncoder;
    private final CuentaRepository cuentas;
    private final SecureRandom secureRandom = new SecureRandom();
    private final JavaMailSender mailSender;
    private final String remitente;
    private static final Logger log =
            LoggerFactory.getLogger(CodigoTemporalService.class);

    /** Recibe los repositorios, el codificador de contraseñas y el servicio de correo. */
    public CodigoTemporalService(
            CodigoTemporalRepository codigoTemporal,
            CuentaRepository cuentas,
            PasswordEncoder passwordEncoder,
            JavaMailSender javaMailSender,
            @Value("${spring.mail.username}") String remitente
    ) {
        this.codigoTemporal = codigoTemporal;
        this.cuentas = cuentas;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = javaMailSender;
        this.remitente = remitente;
    }

    /**
     * Comprueba la vigencia y los intentos del código. Si es correcto, guarda
     * el nuevo hash y consume el código en la misma transacción.
     * Devuelve el ID de la cuenta solo cuando el cambio se completa.
     */
    @Transactional
    public Optional<Integer> establecerContrasena(
            String correo, String codigo, String nuevaContrasena
    ) {
       CuentaRepository.CuentaAcceso cuenta =
               cuentas.buscarPorCorreo(correo).orElse(null);

        if (cuenta == null) {
            return Optional.empty();
        }

        if ("USER".equals(cuenta.rol())
                && !"ACTIVO".equals(cuenta.estadoSocio())) {
            return Optional.empty();
        }

        CodigoTemporalRepository.CodigoPendiente pendiente =
                codigoTemporal.buscarParaActualizar(cuenta.id()).orElse(null);

        if (pendiente == null) {
            return Optional.empty();
        }

        if (!pendiente.caducaEn().isAfter(Instant.now())) {
            cuentas.eliminarCodigoPendiente(cuenta.id());
            return Optional.empty();
        }

        if (pendiente.intentos() >= 3) {
            cuentas.eliminarCodigoPendiente(cuenta.id());
            return Optional.empty();
        }

        if (!passwordEncoder.matches(codigo, pendiente.codigoProtegido())) {
            if (pendiente.intentos() >= 2) {
                cuentas.eliminarCodigoPendiente(cuenta.id());
            } else {
                codigoTemporal.incrementarIntentos(cuenta.id());
            }
            return Optional.empty();
        }

        String nuevaContrasenaProtegida = passwordEncoder.encode(nuevaContrasena);
        cuentas.actualizarContrasenaHash(cuenta.id(), nuevaContrasenaProtegida);
        cuentas.eliminarCodigoPendiente(cuenta.id());

        return Optional.of(cuenta.id());
    }

    /** Genera seis cifras aleatorias, conservando los posibles ceros iniciales. */
    private String generarCodigo() {
        int numero = secureRandom.nextInt(1_000_000);

        return String.format(Locale.ROOT, "%06d", numero);
    }

    /** Guarda el hash de un código nuevo y devuelve el código para enviarlo. */
    private String generarYGuardar(int cuentaId) {
        String codigo = generarCodigo();
        String codigoProtegido = passwordEncoder.encode(codigo);

        codigoTemporal.guardar(cuentaId, codigoProtegido);

        return codigo;
    }

    /**
     * Genera un código para la cuenta e intenta enviarlo al correo indicado.
     * Registra el fallo si el proveedor de correo rechaza el envío.
     */
    public void solicitarCodigo(int cuentaId, String destinatario) {
        String codigo = generarYGuardar(cuentaId);
        StringBuilder texto = new StringBuilder(
                "Has solicitado establecer o recuperar tu contraseña.\n"
                        + "Tu código de seguridad es: "

        );
        texto.append(codigo)
                .append("\nCódigo válido durante 15 minutos.");

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Código para establecer o recuperar la contraseña - Club Pádel Costa Brava");
        mensaje.setText(texto.toString());

        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el código de acceso: {}",
                    e.getClass().getSimpleName());
        }
    }
}

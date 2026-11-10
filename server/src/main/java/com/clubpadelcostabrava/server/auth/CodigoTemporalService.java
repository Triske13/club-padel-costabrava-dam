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

/**
 * Genera y envía códigos temporales y comprueba su uso
 * para establecer o recuperar contraseñas.
 */
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

    /**
     * Recibe el acceso a códigos y cuentas, el codificador de contraseñas
     * y el servicio de correo.
     *
     * @param codigoTemporal  almacenamiento de códigos temporales
     * @param cuentas         acceso a las cuentas
     * @param passwordEncoder protección y comprobación de códigos y contraseñas
     * @param javaMailSender  envío de mensajes por SMTP
     * @param remitente       dirección desde la que se envían los mensajes
     */
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
     * Comprueba un código vigente con menos de tres intentos fallidos.
     * Si es correcto, guarda el hash de la nueva contraseña y consume
     * el código dentro de una misma transacción.
     *
     * @param correo          correo normalizado de la cuenta
     * @param codigo          código introducido por el usuario
     * @param nuevaContrasena nueva contraseña previamente validada
     * @return identificador de la cuenta si se completó el cambio,
     * o un Optional vacío si no se admite el código
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

    /**
     * Genera un código aleatorio de seis cifras.
     *
     * @return código como texto, incluidos los posibles ceros iniciales
     */
    private String generarCodigo() {
        int numero = secureRandom.nextInt(1_000_000);

        return String.format(Locale.ROOT, "%06d", numero);
    }

    /**
     * Genera un código y guarda únicamente su hash en la base de datos.
     * Un código anterior de la misma cuenta queda sustituido.
     *
     * @param cuentaId identificador de la cuenta
     * @return código sin proteger para enviarlo por correo
     */
    private String generarYGuardar(int cuentaId) {
        String codigo = generarCodigo();
        String codigoProtegido = passwordEncoder.encode(codigo);

        codigoTemporal.guardar(cuentaId, codigoProtegido);

        return codigo;
    }

    /**
     * Genera un código temporal e intenta enviarlo al destinatario.
     * Si falla el proveedor de correo, registra el fallo sin propagarlo.
     *
     * @param cuentaId     identificador de la cuenta
     * @param destinatario correo al que se intenta enviar el código
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

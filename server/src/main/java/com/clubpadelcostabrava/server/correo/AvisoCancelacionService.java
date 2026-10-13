package com.clubpadelcostabrava.server.correo;

import com.clubpadelcostabrava.server.reserva.ReservaRepository.ReservaCancelada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvisoCancelacionService {
    private final JavaMailSender mailSender;
    private final String remitente;
    private static final Logger log =
            LoggerFactory.getLogger(AvisoCancelacionService.class);

    public AvisoCancelacionService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String remitente
    ) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public boolean avisarInactivacion(
            String destinatario, List<ReservaCancelada> canceladas
    ) {
        StringBuilder texto = new StringBuilder(
                "Sus reservas futuras han sido canceladas porque su ficha "
                        + "de socio ha pasado a estado INACTIVO.\n"
                        + "Motivo: cambio de estado del socio.\n\n"
                        + "Reservas canceladas:\n"
        );

        for (ReservaCancelada reserva : canceladas) {
            texto.append("- Pista ").append(reserva.pistaId())
                    .append(", ").append(reserva.fecha())
                    .append(", de ").append(reserva.horaInicio())
                    .append(" a ")
                    .append(reserva.horaInicio().plusHours(reserva.duracion()))
                    .append("\n");
        }

        texto.append("\nPara más información, contacte con el club.");

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Cancelación de reservas - Club Pádel Costa Brava");
        mensaje.setText(texto.toString());

        try {
            mailSender.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el aviso de cancelación por inactivación.", e);
            return false;
        }
    }
}

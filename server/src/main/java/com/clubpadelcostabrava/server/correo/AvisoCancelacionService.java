package com.clubpadelcostabrava.server.correo;

import com.clubpadelcostabrava.server.reserva.ReservaRepository.ReservaCancelada;
import com.clubpadelcostabrava.server.reserva.ReservaRespuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

/** Prepara y envía el aviso de reservas canceladas por inactivación. */
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

    /** Devuelve si el servidor de correo aceptó el mensaje; registra los fallos. */
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

    /** Intenta avisar al socio de una cancelación hecha por ADMIN. */
    public boolean avisarCancelacionAdmin(
            String destinatario,
            ReservaRespuesta reserva
    ) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Cancelación de reserva - Club Pádel Costa Brava");
        mensaje.setText(
                "Su reserva ha sido cancelada por administración.\n\n"
                        + "Pista: " + reserva.pistaNombre() + "\n"
                        + "Fecha: " + reserva.fecha() + "\n"
                        + "Horario: " + reserva.horaInicio()
                        + " a " + reserva.horaFin() + "\n"
                        + "Motivo: " + reserva.motivoCancelacion() + "\n\n"
                        + "Para más información, contacte con el club."
        );

        try {
            mailSender.send(mensaje);
            return true;
        } catch (MailException e) {
            log.warn("No se pudo enviar el aviso de cancelación administrativa.", e);
            return false;
        }
    }
}

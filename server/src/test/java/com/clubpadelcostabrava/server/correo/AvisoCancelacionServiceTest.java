package com.clubpadelcostabrava.server.correo;

import com.clubpadelcostabrava.server.reserva.ReservaRespuesta;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AvisoCancelacionServiceTest {

    @Test
    void informaDelFalloSiElCorreoNoSale() {
        JavaMailSender emisor = mock(JavaMailSender.class);
        doThrow(new MailSendException("Fallo simulado"))
                .when(emisor).send(any(SimpleMailMessage.class));

        AvisoCancelacionService avisos =
                new AvisoCancelacionService(emisor, "club@example.invalid");

        ReservaRespuesta reserva = new ReservaRespuesta(
                8, 3, "Socio de prueba",
                2, "Pista 2",
                LocalDate.of(2026, 10, 29),
                "10:00", "12:00", 2,
                "CANCELADA",
                OffsetDateTime.parse("2026-10-26T01:00:00Z"),
                OffsetDateTime.parse("2026-10-26T02:00:00Z"),
                "ADMIN", "Cambio de horario", false
        );

        assertFalse(avisos.avisarCancelacionAdmin(
                "socio@example.invalid", reserva
        ));
    }
}

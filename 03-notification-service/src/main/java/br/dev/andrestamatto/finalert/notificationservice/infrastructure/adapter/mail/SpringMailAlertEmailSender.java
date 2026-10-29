package br.dev.andrestamatto.finalert.notificationservice.infrastructure.adapter.mail;

import br.dev.andrestamatto.finalert.notificationservice.application.ports.in.messaging.AlertTriggeredMessage;
import br.dev.andrestamatto.finalert.notificationservice.application.ports.out.AlertEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SpringMailAlertEmailSender implements AlertEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SpringMailAlertEmailSender(
            JavaMailSender mailSender,
            @Value("${finalert.notification.from-address}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(AlertTriggeredMessage event) {
        var email = new SimpleMailMessage();
        email.setFrom(fromAddress);
        email.setTo(event.userEmail());
        email.setSubject("FinAlert: alerta de %s atingido".formatted(event.symbol()));
        email.setText("""
                Seu alerta de mercado foi atingido.

                Ativo: %s
                Condição: preço %s %s
                Preço observado: %s
                Cotação: %s
                """.formatted(
                event.symbol(),
                event.triggerOperator(),
                event.targetPrice().toPlainString(),
                event.currentPrice().toPlainString(),
                event.quotedAt()
        ));
        mailSender.send(email);
    }
}

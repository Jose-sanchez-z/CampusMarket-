package com.campusmarket.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(JavaMailSender mailSender,
                        @Value("${campusmarket.correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviarToken(String destino, String token, long minutosVigencia) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remitente);
        msg.setTo(destino);
        msg.setSubject("CampusMarket - Código de verificación");
        msg.setText("Tu código de verificación es: " + token
                + "\nVence en " + minutosVigencia + " minutos.");
        mailSender.send(msg);
    }
}

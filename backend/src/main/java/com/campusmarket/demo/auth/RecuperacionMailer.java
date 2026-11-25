package com.campusmarket.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Correo de recuperación (el EmailService de S1 dice "código de verificación"; aquí el texto es otro). */
@Component
public class RecuperacionMailer {

    private final JavaMailSender mailSender;
    private final String remitente;

    public RecuperacionMailer(JavaMailSender mailSender,
                              @Value("${campusmarket.correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(String destino, String token, long minutosVigencia) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remitente);
        msg.setTo(destino);
        msg.setSubject("CampusMarket - Recuperación de contraseña");
        msg.setText("Tu token de recuperación es: " + token
                + "\nVence en " + minutosVigencia + " minutos."
                + "\nSi no lo solicitaste, ignora este correo.");
        mailSender.send(msg);
    }
}

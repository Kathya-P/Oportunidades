/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 *
 * @author kathy
 */
@Service
public class EmailService {
    
    @Autowired
private SpringTemplateEngine templateEngine;
    
    @Autowired
    private JavaMailSender mailSender;

public void enviarAlertaAusentismo(String destino, String nombreEstudiante, long ausencias, String nivel) {
    try {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

        helper.setTo(destino);
        helper.setSubject("⚠ Alerta de ausentismo — " + nombreEstudiante);

        String contenido = "<div style='font-family:Segoe UI,sans-serif;max-width:500px;margin:auto;'>"
            + "<h2 style='color:#2c3e50;'>Programa Oportunidades</h2>"
            + "<p>Se ha detectado un nivel de riesgo <strong>" + nivel + "</strong> para el siguiente estudiante:</p>"
            + "<table style='width:100%;margin:1rem 0;border-collapse:collapse;'>"
            + "<tr><td style='padding:8px;color:#666;'>Estudiante</td><td style='padding:8px;font-weight:600;'>" + nombreEstudiante + "</td></tr>"
            + "<tr style='background:#fafafa;'><td style='padding:8px;color:#666;'>Ausencias</td><td style='padding:8px;color:#c0392b;font-weight:600;'>" + ausencias + "</td></tr>"
            + "<tr><td style='padding:8px;color:#666;'>Nivel</td><td style='padding:8px;'>" + nivel + "</td></tr>"
            + "</table>"
            + "<p style='color:#888;font-size:13px;'>Este es un mensaje automático del Sistema de Asistencia FGK.</p>"
            + "</div>";

        helper.setText(contenido, true);
        mailSender.send(mensaje);

    } catch (Exception e) {
        System.err.println("Error enviando alerta de ausentismo: " + e.getMessage());
    }
}
    
    public void enviarPassword(String destino, String password, String nombre) {

    try {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

        helper.setTo(destino);
        helper.setSubject("Restablecimiento de contraseña");

        // 🔥 Thymeleaf
        Context context = new Context();
        context.setVariable("password", password);
        context.setVariable("nombre", nombre);

        String contenido = templateEngine.process("email/password-reset", context);

        helper.setText(contenido, true);

        mailSender.send(mensaje);

    } catch (Exception e) {
        throw new RuntimeException("Error enviando correo", e);
    }
}
}

package AsistenciaFGK.Oportunidades.services;

import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private JavaMailSender mailSender;

    // ─── Alerta de falta individual del día (notificación inmediata) ────────
    public void enviarFaltaDelDia(String destino, String nombreEstudiante,
                                   String nombreGrupo, String fecha) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

            helper.setTo(destino);
            helper.setSubject("📋 Falta registrada — " + nombreEstudiante);

            String contenido = "<div style='font-family:Segoe UI,sans-serif;max-width:520px;margin:auto;'>"
                + "<div style='background:#C8102E;padding:18px 24px;border-radius:8px 8px 0 0;'>"
                + "<h2 style='color:white;margin:0;font-size:16px;'>Programa Oportunidades — Falta registrada</h2>"
                + "</div>"
                + "<div style='border:1px solid #eee;border-top:none;padding:20px 24px;border-radius:0 0 8px 8px;'>"
                + "<p style='color:#444;margin-bottom:16px;'>El siguiente estudiante <strong>no se presentó</strong> "
                + "al finalizar el horario de su sección:</p>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='padding:8px 12px;color:#888;font-size:13px;border-bottom:1px solid #f0f0f0;'>Estudiante</td>"
                + "<td style='padding:8px 12px;font-weight:600;border-bottom:1px solid #f0f0f0;'>" + nombreEstudiante + "</td></tr>"
                + "<tr><td style='padding:8px 12px;color:#888;font-size:13px;border-bottom:1px solid #f0f0f0;'>Sección / Grupo</td>"
                + "<td style='padding:8px 12px;border-bottom:1px solid #f0f0f0;'>" + nombreGrupo + "</td></tr>"
                + "<tr><td style='padding:8px 12px;color:#888;font-size:13px;'>Fecha</td>"
                + "<td style='padding:8px 12px;'>" + fecha + "</td></tr>"
                + "</table>"
                + "<p style='color:#999;font-size:12px;margin-top:20px;'>Este mensaje fue generado automáticamente "
                + "por el Sistema de Asistencia FGK al cierre del horario de la sección.</p>"
                + "</div></div>";

            helper.setText(contenido, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo de falta del día: " + e.getMessage());
        }
    }

    // ─── Alerta de ausentismo acumulado ─────────────────────────────────────
    public void enviarAlertaAusentismo(String destino, String nombreEstudiante,
                                        long ausencias, String nivel) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

            helper.setTo(destino);
            helper.setSubject("⚠ Alerta de ausentismo — " + nombreEstudiante);

            String contenido = "<div style='font-family:Segoe UI,sans-serif;max-width:500px;margin:auto;'>"
                + "<h2 style='color:#2c3e50;'>Programa Oportunidades</h2>"
                + "<p>Se ha detectado un nivel de riesgo <strong>" + nivel + "</strong> para el siguiente estudiante:</p>"
                + "<table style='width:100%;margin:1rem 0;border-collapse:collapse;'>"
                + "<tr><td style='padding:8px;color:#666;'>Estudiante</td>"
                + "<td style='padding:8px;font-weight:600;'>" + nombreEstudiante + "</td></tr>"
                + "<tr style='background:#fafafa;'><td style='padding:8px;color:#666;'>Ausencias</td>"
                + "<td style='padding:8px;color:#c0392b;font-weight:600;'>" + ausencias + "</td></tr>"
                + "<tr><td style='padding:8px;color:#666;'>Nivel</td>"
                + "<td style='padding:8px;'>" + nivel + "</td></tr>"
                + "</table>"
                + "<p style='color:#888;font-size:13px;'>Este es un mensaje automático del Sistema de Asistencia FGK.</p>"
                + "</div>";

            helper.setText(contenido, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando alerta de ausentismo: " + e.getMessage());
        }
    }

    // ─── Envío de contraseña ─────────────────────────────────────────────────
    public void enviarPassword(String destino, String password, String nombre) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

            helper.setTo(destino);
            helper.setSubject("Restablecimiento de contraseña");

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
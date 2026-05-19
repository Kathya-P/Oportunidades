package AsistenciaFGK.Oportunidades.services;

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private JavaMailSender mailSender;

    // ─── Resumen de faltas del día (una sola notificación + PDF) ───────────
    public void enviarFaltasDelDiaResumen(String destino,
                                          String fecha,
                                          Map<String, List<String>> ausentesPorSeccion) {
        try {
            if (ausentesPorSeccion == null || ausentesPorSeccion.isEmpty()) return;

            int totalAusentes = ausentesPorSeccion.values().stream().mapToInt(List::size).sum();

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

            helper.setTo(destino);
            helper.setSubject("📋 Resumen de faltas — " + fecha + " (" + totalAusentes + ")");

            String contenidoHtml = construirHtmlResumenFaltas(fecha, ausentesPorSeccion, totalAusentes);
            helper.setText(contenidoHtml, true);

            byte[] pdf = construirPdfResumenFaltas(fecha, ausentesPorSeccion, totalAusentes);
            if (pdf != null && pdf.length > 0) {
                helper.addAttachment("faltas-" + fecha + ".pdf", new ByteArrayResource(pdf));
            }

            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando resumen de faltas: " + e.getMessage());
        }
    }

    private String construirHtmlResumenFaltas(String fecha,
                                              Map<String, List<String>> ausentesPorSeccion,
                                              int totalAusentes) {
        StringBuilder bloques = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : ausentesPorSeccion.entrySet()) {
            String seccion = entry.getKey();
            List<String> ausentes = entry.getValue();

            bloques.append("<div style='margin-top:16px;padding:14px 14px;border:1px solid #f0f0f0;border-radius:10px;'>");
            bloques.append("<div style='font-weight:700;color:#111;margin-bottom:8px;'>")
                    .append(seccion)
                    .append(" <span style='color:#666;font-weight:600;'>(")
                    .append(ausentes != null ? ausentes.size() : 0)
                    .append(")</span></div>");

            bloques.append("<table style='width:100%;border-collapse:collapse;'>");
            if (ausentes != null) {
                for (int i = 0; i < ausentes.size(); i++) {
                    String nombre = ausentes.get(i);
                    bloques.append("<tr>")
                            .append("<td style='padding:8px 10px;color:#888;font-size:13px;border-bottom:1px solid #f7f7f7;width:44px;'>")
                            .append(i + 1)
                            .append("</td>")
                            .append("<td style='padding:8px 10px;border-bottom:1px solid #f7f7f7;font-weight:600;'>")
                            .append(nombre)
                            .append("</td>")
                            .append("</tr>");
                }
            }
            bloques.append("</table>");
            bloques.append("</div>");
        }

        return "<div style='font-family:Segoe UI,sans-serif;max-width:720px;margin:auto;'>"
                + "<div style='background:#C8102E;padding:18px 24px;border-radius:8px 8px 0 0;'>"
                + "<h2 style='color:white;margin:0;font-size:16px;'>Programa Oportunidades — Resumen de faltas</h2>"
                + "</div>"
                + "<div style='border:1px solid #eee;border-top:none;padding:20px 24px;border-radius:0 0 8px 8px;'>"
                + "<p style='color:#444;margin-bottom:10px;'>Se registraron <strong>" + totalAusentes + "</strong> faltas el <strong>" + fecha + "</strong>.</p>"
                + "<p style='color:#666;margin-top:0;margin-bottom:14px;'>Detalle por sección:</p>"
                + bloques
                + "<p style='color:#999;font-size:12px;margin-top:20px;'>Este mensaje fue generado automáticamente por el Sistema de Asistencia FGK.</p>"
                + "</div></div>";
    }

    private byte[] construirPdfResumenFaltas(String fecha,
                                             Map<String, List<String>> ausentesPorSeccion,
                                             int totalAusentes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(new Rectangle(595, 842), 48, 48, 54, 48); // A4 aprox
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titulo = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);
            Font meta = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            Font header = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font cell = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

            Paragraph pTitulo = new Paragraph("Programa Oportunidades — Resumen de faltas", titulo);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitulo);

            Paragraph pMeta = new Paragraph("Fecha: " + fecha + "    |    Total de ausentes: " + totalAusentes, meta);
            pMeta.setSpacingBefore(10);
            pMeta.setSpacingAfter(14);
            pMeta.setAlignment(Element.ALIGN_CENTER);
            document.add(pMeta);

            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{5.5f, 1.2f, 7.3f});

            tabla.addCell(crearHeaderCell("Sección", header));
            tabla.addCell(crearHeaderCell("#", header));
            tabla.addCell(crearHeaderCell("Alumno", header));

            boolean zebra = false;
            for (Map.Entry<String, List<String>> entry : ausentesPorSeccion.entrySet()) {
                String seccion = entry.getKey();
                List<String> ausentes = entry.getValue();
                if (ausentes == null || ausentes.isEmpty()) continue;

                for (int i = 0; i < ausentes.size(); i++) {
                    String nombre = ausentes.get(i);
                    BaseColor bg = zebra ? new BaseColor(250, 250, 250) : BaseColor.WHITE;

                    tabla.addCell(crearBodyCell(i == 0 ? seccion : "", cell, bg));
                    tabla.addCell(crearBodyCell(String.valueOf(i + 1), cell, bg));
                    tabla.addCell(crearBodyCell(nombre, cell, bg));

                    zebra = !zebra;
                }
            }

            document.add(tabla);

            Paragraph pie = new Paragraph("Generado automáticamente por el Sistema de Asistencia FGK.", meta);
            pie.setSpacingBefore(16);
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Error generando PDF de resumen de faltas: " + e.getMessage());
            return null;
        }
    }

    private PdfPCell crearHeaderCell(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(new BaseColor(238, 238, 238));
        c.setPadding(8f);
        c.setBorderColor(new BaseColor(220, 220, 220));
        return c;
    }

    private PdfPCell crearBodyCell(String texto, Font font, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        c.setBackgroundColor(bg);
        c.setPadding(7f);
        c.setBorderColor(new BaseColor(235, 235, 235));
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

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

    // ─── Alerta de faltas del día (agrupada por grupo) ──────────────────────
    public void enviarFaltasDelDiaAgrupadas(String destino,
                                            String nombreGrupo,
                                            String fecha,
                                            List<String> estudiantesAusentes) {
        try {
            if (estudiantesAusentes == null || estudiantesAusentes.isEmpty()) return;

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

            helper.setTo(destino);
            helper.setSubject("📋 Faltas registradas — " + nombreGrupo + " (" + estudiantesAusentes.size() + ")");

            StringBuilder filas = new StringBuilder();
            for (int i = 0; i < estudiantesAusentes.size(); i++) {
                String nombre = estudiantesAusentes.get(i);
                filas.append("<tr>")
                    .append("<td style='padding:8px 12px;color:#888;font-size:13px;border-bottom:1px solid #f0f0f0;'>")
                    .append(i + 1)
                    .append("</td>")
                    .append("<td style='padding:8px 12px;font-weight:600;border-bottom:1px solid #f0f0f0;'>")
                    .append(nombre)
                    .append("</td>")
                    .append("</tr>");
            }

            String contenido = "<div style='font-family:Segoe UI,sans-serif;max-width:620px;margin:auto;'>"
                + "<div style='background:#C8102E;padding:18px 24px;border-radius:8px 8px 0 0;'>"
                + "<h2 style='color:white;margin:0;font-size:16px;'>Programa Oportunidades — Faltas del día</h2>"
                + "</div>"
                + "<div style='border:1px solid #eee;border-top:none;padding:20px 24px;border-radius:0 0 8px 8px;'>"
                + "<p style='color:#444;margin-bottom:12px;'>Se registraron <strong>" + estudiantesAusentes.size() + "</strong> faltas al cierre del horario del grupo:</p>"
                + "<table style='width:100%;border-collapse:collapse;margin-bottom:14px;'>"
                + "<tr><td style='padding:8px 12px;color:#888;font-size:13px;border-bottom:1px solid #f0f0f0;'>Sección / Grupo</td>"
                + "<td style='padding:8px 12px;font-weight:600;border-bottom:1px solid #f0f0f0;'>" + nombreGrupo + "</td></tr>"
                + "<tr><td style='padding:8px 12px;color:#888;font-size:13px;'>Fecha</td>"
                + "<td style='padding:8px 12px;'>" + fecha + "</td></tr>"
                + "</table>"
                + "<div style='margin:10px 0 6px;color:#444;font-weight:600;'>Estudiantes ausentes</div>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<thead><tr>"
                + "<th style='text-align:left;padding:8px 12px;font-size:12px;color:#9CA3AF;border-bottom:1px solid #f0f0f0;'>#</th>"
                + "<th style='text-align:left;padding:8px 12px;font-size:12px;color:#9CA3AF;border-bottom:1px solid #f0f0f0;'>Nombre</th>"
                + "</tr></thead>"
                + "<tbody>" + filas + "</tbody>"
                + "</table>"
                + "<p style='color:#999;font-size:12px;margin-top:20px;'>Este mensaje fue generado automáticamente por el Sistema de Asistencia FGK al cierre del horario de la sección.</p>"
                + "</div></div>";

            helper.setText(contenido, true);
            mailSender.send(mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando correo agrupado de faltas: " + e.getMessage());
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
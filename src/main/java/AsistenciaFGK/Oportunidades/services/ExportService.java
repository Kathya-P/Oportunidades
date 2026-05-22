package AsistenciaFGK.Oportunidades.services;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import AsistenciaFGK.Oportunidades.models.Asistencia;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
// Nuevos imports que necesitas agregar arriba:
import org.springframework.core.io.ClassPathResource;
import AsistenciaFGK.Oportunidades.models.AsistenciaReporteDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ExportService {

    // ─────────────────────────────────────────────
    // PDF — Asistencias
    // ─────────────────────────────────────────────
    public void exportarAsistenciasPDF(
            List<Asistencia> asistencias,
            String filtro,
            String inicio,
            String fin,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=asistencia-" + filtro + "-" + inicio + ".pdf");

        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        com.itextpdf.text.Font fTitulo = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font fSub    = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, new BaseColor(107, 114, 128));
        com.itextpdf.text.Font fHeader = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9,  com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fCelda  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9,  com.itextpdf.text.Font.NORMAL, new BaseColor(51, 51, 51));
        com.itextpdf.text.Font fFooter = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8,  com.itextpdf.text.Font.ITALIC, new BaseColor(156, 163, 175));

        doc.add(new Paragraph("Programa Oportunidades — FGK", fTitulo));
        doc.add(new Paragraph("Reporte de asistencia: " + filtro.toUpperCase() +
            " (" + inicio + (inicio.equals(fin) ? "" : " al " + fin) + ")", fSub));
        doc.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{3f, 2f, 1.5f, 1.5f, 1.5f});

        BaseColor colorHeader = new BaseColor(26, 26, 26);
        for (String h : new String[]{"Estudiante", "Grupo", "Hora entrada", "Hora salida", "Estado"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(colorHeader);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(cell);
        }

        boolean par = false;
        for (Asistencia a : asistencias) {
            BaseColor fila = par ? new BaseColor(249, 250, 251) : BaseColor.WHITE;
            String[] vals = {
                a.getEstudiante().getNombre() + " " + a.getEstudiante().getApellido(),
                a.getGrupo().getNombre(),
                a.getHoraEntrada() != null ? a.getHoraEntrada() : "—",
                a.getHoraSalida()  != null ? a.getHoraSalida()  : "—",
                a.getEstado()
            };
            for (String v : vals) {
                PdfPCell cell = new PdfPCell(new Phrase(v, fCelda));
                cell.setBackgroundColor(fila);
                cell.setPadding(7);
                cell.setBorder(Rectangle.NO_BORDER);
                tabla.addCell(cell);
            }
            par = !par;
        }

        doc.add(tabla);
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total registros: " + asistencias.size() +
            "   |   Generado: " + java.time.LocalDate.now(), fFooter));
        doc.close();
    }

    // ─────────────────────────────────────────────
    // EXCEL — Asistencias
    // ─────────────────────────────────────────────
    public void exportarAsistenciasExcel(
            List<Asistencia> asistencias,
            String filtro,
            String inicio,
            String fin,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=asistencia-" + filtro + "-" + inicio + ".xlsx");

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Asistencia");

        // Estilo encabezado
        CellStyle estiloHeader = wb.createCellStyle();
        Font fHeader = wb.createFont();
        fHeader.setBold(true);
        fHeader.setColor(IndexedColors.WHITE.getIndex());
        estiloHeader.setFont(fHeader);
        estiloHeader.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloHeader.setBorderBottom(BorderStyle.THIN);

        // Estilo fila par
        CellStyle estiloPar = wb.createCellStyle();
        estiloPar.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloPar.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Título
        Row titulo = sheet.createRow(0);
        titulo.createCell(0).setCellValue("Programa Oportunidades — FGK");
        Row subtitulo = sheet.createRow(1);
        subtitulo.createCell(0).setCellValue(
            "Reporte " + filtro.toUpperCase() + " | " + inicio +
            (inicio.equals(fin) ? "" : " al " + fin));
        sheet.createRow(2); // fila vacía

        // Encabezados
        Row header = sheet.createRow(3);
        String[] cols = {"Estudiante", "Grupo", "Hora entrada", "Hora salida", "Estado"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(estiloHeader);
        }

        // Datos
        int rowNum = 4;
        boolean par = false;
        for (Asistencia a : asistencias) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(a.getEstudiante().getNombre() + " " + a.getEstudiante().getApellido());
            row.createCell(1).setCellValue(a.getGrupo().getNombre());
            row.createCell(2).setCellValue(a.getHoraEntrada() != null ? a.getHoraEntrada() : "—");
            row.createCell(3).setCellValue(a.getHoraSalida()  != null ? a.getHoraSalida()  : "—");
            row.createCell(4).setCellValue(a.getEstado());
            if (par) {
                for (int i = 0; i < 5; i++) row.getCell(i).setCellStyle(estiloPar);
            }
            par = !par;
        }

        // Autoajustar columnas
        for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

        wb.write(response.getOutputStream());
        wb.close();
    }

    // ─────────────────────────────────────────────
    // PDF — Alumnos en riesgo (supervisor)
    // ─────────────────────────────────────────────
    public void exportarRiesgoPDF(
            List<Map<String, Object>> enRiesgo,
            HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment; filename=riesgo-ausentismo-" + java.time.LocalDate.now() + ".pdf");

        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        com.itextpdf.text.Font fTitulo = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font fSub    = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, new BaseColor(107, 114, 128));
        com.itextpdf.text.Font fHeader = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9,  com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fCelda  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9,  com.itextpdf.text.Font.NORMAL, new BaseColor(51, 51, 51));
        com.itextpdf.text.Font fFooter = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8,  com.itextpdf.text.Font.ITALIC, new BaseColor(156, 163, 175));

        doc.add(new Paragraph("Programa Oportunidades — FGK", fTitulo));
        doc.add(new Paragraph("Reporte de riesgo de ausentismo — " + java.time.LocalDate.now(), fSub));
        doc.add(new Paragraph("Criterio: 1 falta = Precaución | 2 = Alerta | 3+ = Crítico", fSub));
        doc.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{3f, 1.5f, 1.5f, 2f});

        BaseColor colorHeader = new BaseColor(26, 26, 26);
        for (String h : new String[]{"Estudiante", "Ausencias", "Tardanzas", "Nivel de riesgo"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(colorHeader);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(cell);
        }

        boolean par = false;
        for (Map<String, Object> r : enRiesgo) {
            BaseColor fila = par ? new BaseColor(249, 250, 251) : BaseColor.WHITE;
            String[] vals = {
                (String) r.get("nombre"),
                String.valueOf(r.get("ausencias")),
                String.valueOf(r.get("tardanzas")),
                (String) r.get("nivel")
            };
            for (String v : vals) {
                PdfPCell cell = new PdfPCell(new Phrase(v, fCelda));
                cell.setBackgroundColor(fila);
                cell.setPadding(7);
                cell.setBorder(Rectangle.NO_BORDER);
                tabla.addCell(cell);
            }
            par = !par;
        }

        doc.add(tabla);
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Total estudiantes en riesgo: " + enRiesgo.size() +
            "   |   Generado: " + java.time.LocalDate.now(), fFooter));
        doc.close();
    }

    // ─────────────────────────────────────────────
    // EXCEL — Alumnos en riesgo (supervisor)
    // ─────────────────────────────────────────────
    public void exportarRiesgoExcel(
            List<Map<String, Object>> enRiesgo,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=riesgo-ausentismo-" + java.time.LocalDate.now() + ".xlsx");

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Riesgo Ausentismo");

        CellStyle estiloHeader = wb.createCellStyle();
        Font fHeader = wb.createFont();
        fHeader.setBold(true);
        fHeader.setColor(IndexedColors.WHITE.getIndex());
        estiloHeader.setFont(fHeader);
        estiloHeader.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle estiloPar = wb.createCellStyle();
        estiloPar.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloPar.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row titulo = sheet.createRow(0);
        titulo.createCell(0).setCellValue("Programa Oportunidades — Riesgo de Ausentismo");
        Row subtitulo = sheet.createRow(1);
        subtitulo.createCell(0).setCellValue("Generado: " + java.time.LocalDate.now());
        sheet.createRow(2);

        Row header = sheet.createRow(3);
        String[] cols = {"Estudiante", "Ausencias", "Tardanzas", "Nivel de riesgo"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(estiloHeader);
        }

        int rowNum = 4;
        boolean par = false;
        for (Map<String, Object> r : enRiesgo) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) r.get("nombre"));
            row.createCell(1).setCellValue((Long) r.get("ausencias"));
            row.createCell(2).setCellValue((Long) r.get("tardanzas"));
            row.createCell(3).setCellValue((String) r.get("nivel"));
            if (par) {
                for (int i = 0; i < 4; i++) row.getCell(i).setCellStyle(estiloPar);
            }
            par = !par;
        }

        for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

        wb.write(response.getOutputStream());
        wb.close();
    }

    // Nuevo método a agregar al final de ExportService:
public void exportarAsistenciasJasper(
        List<Asistencia> asistencias,
        String filtro,
        String inicio,
        String fin,
        HttpServletResponse response) throws Exception {

    // 1. Convertir las asistencias al DTO que Jasper entiende
    List<AsistenciaReporteDTO> datos = asistencias.stream()
        .map(a -> new AsistenciaReporteDTO(
            a.getEstudiante().getNombre() + " " + a.getEstudiante().getApellido(),
            a.getGrupo().getNombre(),
            a.getHoraEntrada(),
            a.getHoraSalida(),
            a.getEstado()
        )).toList();

    // 2. Cargar la plantilla .jrxml y compilarla
    InputStream reportStream = new ClassPathResource("reports/asistencias.jrxml").getInputStream();
    JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

    // 3. Pasar los parámetros
    HashMap<String, Object> params = new HashMap<>();
    params.put("filtro", filtro);
    params.put("inicio", inicio);
    params.put("fin", fin);

    // 4. Crear el datasource desde la lista de DTOs
    JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos);

    // 5. Llenar el reporte con datos
    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

    // 6. Exportar como PDF al response
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition",
        "attachment; filename=asistencia-jasper-" + filtro + "-" + inicio + ".pdf");

    JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());
}
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import java.io.ByteArrayOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.services.EstudianteService;
import AsistenciaFGK.Oportunidades.services.SeccionService;
import com.itextpdf.awt.geom.misc.RenderingHints;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/alumnos")
@PreAuthorize("hasRole('ADMIN')")
public class AlumnoController {

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private SeccionService seccionService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("alumnos", estudianteService.listarTodos());
        return "admin/alumnos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("alumno", new Estudiante());
        model.addAttribute("secciones", seccionService.listarTodos());
        model.addAttribute("titulo", "Nuevo alumno");
        return "admin/alumno-form";
    }

@PostMapping("/guardar")
public String guardar(@ModelAttribute Estudiante estudiante,
                      @RequestParam(value = "grupos", required = false) List<Integer> grupoIds,
                      RedirectAttributes redirectAttrs) {

    if (estudiante.getIdEstudiante() == null &&
        estudianteService.existeCodigoBarras(estudiante.getCodigoBarras())) {
        redirectAttrs.addFlashAttribute("error", "El código de barras ya está registrado.");
        return "redirect:/admin/alumnos/nuevo";
    }

    // Asignar grupos seleccionados
    if (grupoIds != null && !grupoIds.isEmpty()) {
        List<Grupo> grupos = grupoIds.stream()
            .map(id -> seccionService.buscarPorId(id).orElse(null))
            .filter(g -> g != null)
            .collect(java.util.stream.Collectors.toList());
        estudiante.setGrupos(grupos);
    } else {
        estudiante.setGrupos(new java.util.ArrayList<>());
    }

    estudianteService.guardar(estudiante);
    redirectAttrs.addFlashAttribute("exito", "Alumno guardado correctamente.");
    return "redirect:/admin/alumnos";
}

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Estudiante estudiante = estudianteService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        model.addAttribute("alumno", estudiante);
        model.addAttribute("secciones", seccionService.listarTodos());
        model.addAttribute("titulo", "Editar alumno");
        return "admin/alumno-form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           RedirectAttributes redirectAttrs) {
        estudianteService.eliminar(id);
        redirectAttrs.addFlashAttribute("exito", "Alumno eliminado.");
        return "redirect:/admin/alumnos";
    }
    
@GetMapping("/{id}/barcode")
public ResponseEntity<byte[]> descargarBarcode(@PathVariable Integer id) throws Exception {

    Estudiante estudiante = estudianteService.buscarPorId(id)
        .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

    String codigo         = estudiante.getCodigoBarras();
    String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellido();

    // 1. Generar el código de barras Code128
    int barcodeWidth  = 400;
    int barcodeHeight = 100;
    Code128Writer writer = new Code128Writer();
    BitMatrix bitMatrix  = writer.encode(codigo, BarcodeFormat.CODE_128, barcodeWidth, barcodeHeight);

    // Convertir BitMatrix a BufferedImage
    BufferedImage barcodeImg = new BufferedImage(barcodeWidth, barcodeHeight, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < barcodeWidth; x++)
        for (int y = 0; y < barcodeHeight; y++)
            barcodeImg.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());

    // 2. Imagen final con espacio extra abajo para el nombre
    int totalHeight = barcodeHeight + 26;
    BufferedImage finalImg = new BufferedImage(barcodeWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = finalImg.createGraphics();

    g.setColor(Color.WHITE);
    g.fillRect(0, 0, barcodeWidth, totalHeight);
    g.drawImage(barcodeImg, 0, 0, null);

    // 3. Nombre centrado, fuente pequeña
    g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, 
                   java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(Color.BLACK);
    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
    FontMetrics fm = g.getFontMetrics();
    g.drawString(nombreCompleto, (barcodeWidth - fm.stringWidth(nombreCompleto)) / 2, barcodeHeight + fm.getAscent() + 2);
    g.dispose();

    // 4. Exportar PNG
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(finalImg, "PNG", baos);

    String nombreArchivo = "barcode-" + codigo + ".png";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
        .contentType(MediaType.IMAGE_PNG)
        .body(baos.toByteArray());
}

@GetMapping("/barcode/seccion/{idGrupo}")
public ResponseEntity<byte[]> descargarBarcodesPorSeccion(@PathVariable Integer idGrupo) throws Exception {

    Grupo grupo = seccionService.buscarPorId(idGrupo)
        .orElseThrow(() -> new RuntimeException("Sección no encontrada"));

    List<Estudiante> alumnos = estudianteService.listarTodos().stream()
        .filter(e -> e.getGrupos() != null &&
                     e.getGrupos().stream().anyMatch(g -> g.getIdGrupo().equals(idGrupo)))
        .collect(java.util.stream.Collectors.toList());

    ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
    ZipOutputStream zip = new ZipOutputStream(zipBaos);

    for (Estudiante estudiante : alumnos) {
        String codigo         = estudiante.getCodigoBarras();
        String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellido();

        int barcodeWidth  = 400;
        int barcodeHeight = 100;
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix  = writer.encode(codigo, BarcodeFormat.CODE_128, barcodeWidth, barcodeHeight);

        BufferedImage barcodeImg = new BufferedImage(barcodeWidth, barcodeHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < barcodeWidth; x++)
            for (int y = 0; y < barcodeHeight; y++)
                barcodeImg.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());

        int totalHeight = barcodeHeight + 26;
        BufferedImage finalImg = new BufferedImage(barcodeWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = finalImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, barcodeWidth, totalHeight);
        g.drawImage(barcodeImg, 0, 0, null);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                           java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(nombreCompleto, (barcodeWidth - fm.stringWidth(nombreCompleto)) / 2,
                     barcodeHeight + fm.getAscent() + 2);
        g.dispose();

        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        ImageIO.write(finalImg, "PNG", imgBaos);

        zip.putNextEntry(new ZipEntry("barcode-" + codigo + ".png"));
        zip.write(imgBaos.toByteArray());
        zip.closeEntry();
    }

    zip.finish();

    String nombreZip = "barcodes-" + grupo.getNombre().replaceAll("\\s+", "_") + ".zip";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreZip + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(zipBaos.toByteArray());
}

}
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
import java.util.List;
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

    String codigo = estudiante.getCodigoBarras();

    // Generar imagen Code128 real
    Code128Writer writer = new Code128Writer();
    BitMatrix bitMatrix = writer.encode(codigo, BarcodeFormat.CODE_128, 400, 120);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
    byte[] imagenBytes = baos.toByteArray();

    // Nombre del archivo: barcode-2021-SA-FT-0043.png
    String nombreArchivo = "barcode-" + codigo + ".png";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
        .contentType(MediaType.IMAGE_PNG)
        .body(imagenBytes);
}
}
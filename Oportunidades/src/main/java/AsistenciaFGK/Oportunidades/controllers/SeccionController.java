/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.controllers;


import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.services.SeccionService;
import AsistenciaFGK.Oportunidades.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/secciones")
@PreAuthorize("hasRole('ADMIN')")
public class SeccionController {

    @Autowired
    private SeccionService seccionService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("secciones", seccionService.listarTodos());
        return "admin/secciones";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("seccion", new Grupo());
        model.addAttribute("titulo", "Nueva sección");
        return "admin/seccion-form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Grupo grupo, RedirectAttributes redirectAttrs) {
        seccionService.guardar(grupo);
        redirectAttrs.addFlashAttribute("exito", "Sección guardada correctamente.");
        return "redirect:/admin/secciones";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Grupo grupo = seccionService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        model.addAttribute("seccion", grupo);
        model.addAttribute("titulo", "Editar sección");
        return "admin/seccion-form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        seccionService.eliminar(id);
        redirectAttrs.addFlashAttribute("exito", "Sección eliminada.");
        return "redirect:/admin/secciones";
    }
}
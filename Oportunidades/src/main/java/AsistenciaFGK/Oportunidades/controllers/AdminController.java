/*
 * AdminController.java — MODIFICADO para incluir datos del calendario
 * en el dashboard.
 *
 * Cambios respecto al original:
 *   1. Se inyecta CalendarioService.
 *   2. El método dashboard() añade los atributos calXxx al modelo.
 */
package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.models.Role;
import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.services.CalendarioService; // ← NUEVO
import AsistenciaFGK.Oportunidades.services.EmailService;
import AsistenciaFGK.Oportunidades.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private AsistenciaFGK.Oportunidades.repositories.EstudianteRepository estudianteRepository;
    @Autowired private AsistenciaFGK.Oportunidades.repositories.GrupoRepository grupoRepository;
    @Autowired private AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository asistenciaRepository;
    @Autowired private CalendarioService calendarioService; // ← NUEVO

    // ── Dashboard ──────────────────────────────────────────────────────────
    @GetMapping
    public String dashboard(Model model) {

        long totalAlumnos  = estudianteRepository.count();
        long totalDocentes = usuarioService.listarTodos().stream()
                                .filter(u -> u.getRole().name().equals("DOCENTE")).count();
        long totalGrupos   = grupoRepository.count();
        long totalUsuarios = usuarioService.listarTodos().size();

        java.util.Date hoy = java.sql.Date.valueOf(LocalDate.now());
        long asistenciasHoy = grupoRepository.findAll().stream()
                                .flatMap(g -> asistenciaRepository.findByGrupoAndFecha(g, hoy).stream())
                                .count();

        model.addAttribute("totalAlumnos",   totalAlumnos);
        model.addAttribute("totalDocentes",  totalDocentes);
        model.addAttribute("totalGrupos",    totalGrupos);
        model.addAttribute("totalUsuarios",  totalUsuarios);
        model.addAttribute("asistenciasHoy", asistenciasHoy);

        // ── NUEVO: datos del widget de calendario ─────────────────────────
        YearMonth ym      = YearMonth.now();
        LocalDate inicio  = ym.atDay(1);
        LocalDate fin     = ym.atEndOfMonth();

        model.addAttribute("calHoy",          LocalDate.now());
        model.addAttribute("calAnio",         ym.getYear());
        model.addAttribute("calMes",          ym.getMonthValue());
        model.addAttribute("calDiasEnMes",    ym.lengthOfMonth());
        model.addAttribute("calPrimerDia",    inicio.getDayOfWeek().getValue() % 7);
        model.addAttribute("calPeriodoActual", calendarioService.periodoActual().orElse(null));
        model.addAttribute("calDiasEsp",      calendarioService.diasEnRango(inicio, fin));
        model.addAttribute("calEsBloqueado",  calendarioService.esHoyDiaBloqueado());
        model.addAttribute("calMotivoBloqueo", calendarioService.motivoBloqueoHoy());
        // ─────────────────────────────────────────────────────────────────

        return "admin/dashboard";
    }

    // ── Lista de usuarios ──────────────────────────────────────────────────
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/nuevo")
    public String nuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles",   Role.values());
        model.addAttribute("titulo",  "Nuevo usuario");
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario,
                                  RedirectAttributes redirectAttrs) {
        if (usuario.getIdUsuario() == null && usuarioService.existeUsername(usuario.getUsername())) {
            redirectAttrs.addFlashAttribute("error", "El username ya existe.");
            return "redirect:/admin/usuarios/nuevo";
        }
        usuarioService.guardar(usuario);
        redirectAttrs.addFlashAttribute("exito", "Usuario guardado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles",   Role.values());
        model.addAttribute("titulo",  "Editar usuario");
        return "admin/usuario-form";
    }

    @GetMapping("/usuarios/reset-password/{id}")
    public String resetPassword(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String nueva = java.util.UUID.randomUUID().toString().substring(0, 8);
        usuario.setPassword(passwordEncoder.encode(nueva));
        usuario.setDebeCambiarPassword(true);
        usuarioService.guardar(usuario);
        emailService.enviarPassword(usuario.getEmail(), nueva, usuario.getNombre());
        redirectAttrs.addFlashAttribute("exito", "Contraseña enviada al correo del usuario");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminar(id);
        redirectAttrs.addFlashAttribute("exito", "Usuario eliminado.");
        return "redirect:/admin/usuarios";
    }
}
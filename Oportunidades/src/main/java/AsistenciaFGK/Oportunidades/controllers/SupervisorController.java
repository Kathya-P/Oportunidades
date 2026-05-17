package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.services.CalendarioService;
import AsistenciaFGK.Oportunidades.services.ExportService;
import AsistenciaFGK.Oportunidades.services.SupervisorService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/supervisor")
@PreAuthorize("hasRole('SUPERVISOR')")
public class SupervisorController {

    @Autowired
    private SupervisorService supervisorService;

    @Autowired
    private EstudianteRepository estudianteRepo;
    
    @Autowired
private ExportService exportService;

    @Autowired
    private CalendarioService calendarioService;

    // ── Dashboard con estadísticas globales ────────────────────
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", supervisorService.obtenerEstadisticasGlobales());
        model.addAttribute("porGrupo", supervisorService.obtenerEstadisticasPorGrupo());
        model.addAttribute("enRiesgo", supervisorService.detectarRiesgoAusentismo());

        // Datos del widget de calendario (mes actual)
        YearMonth ym = YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();
        model.addAttribute("calHoy", LocalDate.now());
        model.addAttribute("calAnio", ym.getYear());
        model.addAttribute("calMes", ym.getMonthValue());
        model.addAttribute("calDiasEnMes", ym.lengthOfMonth());
        model.addAttribute("calPrimerDia", inicio.getDayOfWeek().getValue() % 7);
        model.addAttribute("calPeriodoActual", calendarioService.periodoActual().orElse(null));
        model.addAttribute("calDiasEsp", calendarioService.diasEnRango(inicio, fin));
        model.addAttribute("calEsBloqueado", calendarioService.esHoyDiaBloqueado());
        model.addAttribute("calMotivoBloqueo", calendarioService.motivoBloqueoHoy());

        return "supervisor/dashboard";
    }

    // ── Ver todas las asistencias ──────────────────────────────
    @GetMapping("/asistencias")
public String verAsistencias(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
        Model model) {

    java.time.LocalDate fechaConsultada = fecha != null ? fecha : java.time.LocalDate.now();
    boolean modoHistorial = fecha != null;

    model.addAttribute("asistencias", supervisorService.listarAsistenciasPorFecha(fechaConsultada));
    model.addAttribute("grupos", supervisorService.listarGrupos());
    model.addAttribute("fechaConsultada", java.sql.Date.valueOf(fechaConsultada));
    model.addAttribute("fechaSeleccionada", fechaConsultada.toString());
    model.addAttribute("modoHistorial", modoHistorial);
    // La plantilla viene de una versión "docente" y espera estas variables.
    // El supervisor actualmente trabaja por fecha puntual (no rango).
    model.addAttribute("modoRango", false);
    model.addAttribute("fechaDesdeStr", "");
    model.addAttribute("fechaHastaStr", "");
    return "supervisor/asistencias";
}

    // ── Riesgo de ausentismo ───────────────────────────────────
    @GetMapping("/riesgo")
    public String verRiesgo(Model model) {
        model.addAttribute("enRiesgo", supervisorService.detectarRiesgoAusentismo());
        model.addAttribute("periodoActual", calendarioService.periodoActual().orElse(null));
        return "supervisor/riesgo";
    }

    // ── Ver estudiantes (solo lectura) ─────────────────────────
    @GetMapping("/usuarios")
public String verEstudiantes(Model model) {
    // Vista deshabilitada: el supervisor ahora usa solo Dashboard/Asistencias/Riesgo.
    return "redirect:/supervisor";
}
    
    @GetMapping("/riesgo/exportar")
public void exportarRiesgo(
        @RequestParam String formato,
        HttpServletResponse response) throws Exception {

    List<Map<String, Object>> enRiesgo = supervisorService.detectarRiesgoAusentismo();

    if ("excel".equals(formato)) {
        exportService.exportarRiesgoExcel(enRiesgo, response);
    } else {
        exportService.exportarRiesgoPDF(enRiesgo, response);
    }
}
}
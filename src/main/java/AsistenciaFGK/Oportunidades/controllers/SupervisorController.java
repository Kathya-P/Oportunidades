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

    List<Map<String, Object>> enRiesgo = supervisorService.detectarRiesgoAusentismo();

    Map<Integer, Map<String, Object>> riesgoMap = new HashMap<>();
    for (Map<String, Object> r : enRiesgo) {
        Object idObj = r.get("idEstudiante");
        if (idObj instanceof Integer id) {
            riesgoMap.put(id, r);
        }
    }

    model.addAttribute("estudiantes", estudianteRepo.findAll());
    model.addAttribute("riesgoMap", riesgoMap);

    return "supervisor/usuarios";
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
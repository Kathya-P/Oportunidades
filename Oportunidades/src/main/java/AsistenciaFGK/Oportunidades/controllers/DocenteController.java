package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.models.Asistencia;
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import AsistenciaFGK.Oportunidades.repositories.UsuarioRepository;
import AsistenciaFGK.Oportunidades.services.CalendarioService;
import AsistenciaFGK.Oportunidades.services.ExportService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/docente")
@PreAuthorize("hasRole('DOCENTE')")
public class DocenteController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private CalendarioService calendarioService;

    private void agregarWidgetCalendario(Model model) {
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
    }

    // ─────────────────────────────────────────────
    // VISTA HOY
    // ─────────────────────────────────────────────
    @GetMapping
    public String vistaHoy(Model model, Principal principal) {

        Usuario docente = usuarioRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        List<Grupo> grupos = grupoRepository.findAll();

        Date hoy = java.sql.Date.valueOf(LocalDate.now());

        List<Asistencia> asistencias = grupos.stream()
                .flatMap(g ->
                        asistenciaRepository
                                .findByGrupoAndFecha(g, hoy)
                                .stream()
                )
                .toList();

        model.addAttribute("docente", docente);
        model.addAttribute("grupos", grupos);
        model.addAttribute("asistencias", asistencias);

        model.addAttribute("fechaConsultada", hoy);
        model.addAttribute("fechaSeleccionada", LocalDate.now().toString());

        model.addAttribute("modoHistorial", false);
        model.addAttribute("modoRango", false);

        model.addAttribute("fechaDesdeStr", null);
        model.addAttribute("fechaHastaStr", null);

        model.addAttribute("seccionesSeleccionadas", List.of());

        agregarWidgetCalendario(model);

        return "docente/asistencias";
    }
    
    // ─────────────────────────────────────────────
    // HISTORIAL
    // ─────────────────────────────────────────────
    @GetMapping("/historial")
    public String vistaHistorial(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(required = false)
            List<Integer> secciones,

            Model model,
            Principal principal
    ) {

        Usuario docente = usuarioRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        List<Grupo> grupos = grupoRepository.findAll();

        boolean modoRango = (desde != null && hasta != null);

        List<Asistencia> asistencias;

        String desdeStr = null;
        String hastaStr = null;

        if (modoRango) {

            if (hasta.isBefore(desde)) {
                LocalDate tmp = desde;
                desde = hasta;
                hasta = tmp;
            }

            desdeStr = desde.toString();
            hastaStr = hasta.toString();

            Date inicio = java.sql.Date.valueOf(desde);
            Date fin = java.sql.Date.valueOf(hasta);

            List<Grupo> gruposFiltrados;

            if (secciones != null && !secciones.isEmpty()) {
                gruposFiltrados = grupoRepository.findAllById(secciones);
            } else {
                gruposFiltrados = grupos;
            }

            asistencias = gruposFiltrados.stream()
                    .flatMap(g ->
                            asistenciaRepository
                                    .findByGrupoAndFechaBetween(g, inicio, fin)
                                    .stream()
                    )
                    .toList();

        } else {

            asistencias = List.of();
        }

        model.addAttribute("docente", docente);
        model.addAttribute("grupos", grupos);
        model.addAttribute("asistencias", asistencias);

        model.addAttribute("modoHistorial", true);
        model.addAttribute("modoRango", modoRango);

        model.addAttribute("fechaDesdeStr", desdeStr);
        model.addAttribute("fechaHastaStr", hastaStr);

        model.addAttribute("fechaSeleccionada", desdeStr);

        model.addAttribute(
                "fechaConsultada",
                desde != null
                        ? java.sql.Date.valueOf(desde)
                        : java.sql.Date.valueOf(LocalDate.now())
        );

        model.addAttribute(
                "seccionesSeleccionadas",
                secciones != null ? secciones : List.of()
        );

        agregarWidgetCalendario(model);

        return "docente/asistencias";
    }

    // ─────────────────────────────────────────────
// EXPORTAR
// ─────────────────────────────────────────────
@GetMapping("/exportar")
public void exportar(

        @RequestParam String filtro,

        @RequestParam String formato,

        @RequestParam(required = false)
        List<Integer> secciones,

        @RequestParam(required = false)
        String grupo,

        @RequestParam(required = false)
        Integer idEstudiante,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fecha,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,

        HttpServletResponse response

) throws Exception {

    LocalDate hoy = LocalDate.now();

    LocalDate inicio = hoy;
    LocalDate fin    = hoy;

    switch (filtro) {

        case "rango", "alumno" -> {
            inicio = desde != null ? desde : hoy;
            fin    = hasta != null ? hasta : hoy;
            if (fin.isBefore(inicio)) {
                LocalDate t = inicio;
                inicio = fin;
                fin = t;
            }
        }

        default -> {
            inicio = fecha != null ? fecha : hoy;
            fin    = inicio;
        }
    }

    Date dInicio = java.sql.Date.valueOf(inicio);
    Date dFin    = java.sql.Date.valueOf(fin);

    // ─────────────────────────────────────────
    // RESOLVER ASISTENCIAS
    // Prioridad: alumno > grupo > secciones > todos
    // ─────────────────────────────────────────
    List<Asistencia> asistencias;
    String etiqueta;

    if (idEstudiante != null) {
        // Exportar asistencia de un alumno específico en el rango
        asistencias = asistenciaRepository
                .findByEstudiante_IdEstudiante(idEstudiante)
                .stream()
                .filter(a -> a.getFecha() != null
                          && !a.getFecha().before(dInicio)
                          && !a.getFecha().after(dFin))
                .toList();
        etiqueta = "alumno-" + idEstudiante;

    } else if (grupo != null && !grupo.isBlank()) {
        // Exportar un grupo específico por nombre
        asistencias = grupoRepository.findByNombre(grupo)
                .map(g -> asistenciaRepository.findByGrupoAndFechaBetween(g, dInicio, dFin))
                .orElse(List.of());
        etiqueta = "grupo-" + grupo;

    } else if (secciones != null && !secciones.isEmpty()) {
        // Exportar secciones seleccionadas por ID
        asistencias = grupoRepository.findAllById(secciones).stream()
                .flatMap(g -> asistenciaRepository
                        .findByGrupoAndFechaBetween(g, dInicio, dFin)
                        .stream())
                .toList();
        etiqueta = filtro;

    } else {
        // Exportar todos los grupos
        asistencias = grupoRepository.findAll().stream()
                .flatMap(g -> asistenciaRepository
                        .findByGrupoAndFechaBetween(g, dInicio, dFin)
                        .stream())
                .toList();
        etiqueta = filtro;
    }

    // ─────────────────────────────────────────
    // EXPORTAR
    // ─────────────────────────────────────────
    if ("excel".equals(formato)) {
        exportService.exportarAsistenciasExcel(
                asistencias,
                etiqueta,
                inicio.toString(),
                fin.toString(),
                response
        );
    } else {
        exportService.exportarAsistenciasPDF(
                asistencias,
                etiqueta,
                inicio.toString(),
                fin.toString(),
                response
        );
    }
}
}
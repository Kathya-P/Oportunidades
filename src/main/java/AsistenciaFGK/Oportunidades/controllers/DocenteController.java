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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/docente")
@PreAuthorize("hasRole('DOCENTE')")
public class DocenteController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private AsistenciaRepository asistenciaRepository;
    @Autowired private ExportService exportService;
    @Autowired private CalendarioService calendarioService;

    // ─────────────────────────────────────────────────────────────
    // Mapa de DayOfWeek → string para comparar con grupo.getDias()
    // ─────────────────────────────────────────────────────────────
    private static final Map<DayOfWeek, String> DIA_MAP = Map.of(
        DayOfWeek.MONDAY,    "LUNES",
        DayOfWeek.TUESDAY,   "MARTES",
        DayOfWeek.WEDNESDAY, "MIERCOLES",
        DayOfWeek.THURSDAY,  "JUEVES",
        DayOfWeek.FRIDAY,    "VIERNES",
        DayOfWeek.SATURDAY,  "SABADO",
        DayOfWeek.SUNDAY,    "DOMINGO"
    );

    // ─────────────────────────────────────────────────────────────
    // Devuelve los grupos que tienen clase HOY (el día coincide),
    // sin importar si la hora actual está dentro del rango o no.
    // ─────────────────────────────────────────────────────────────
    // ✅ CORRECTO — valida día Y que la hora actual esté dentro del rango
private List<Grupo> gruposActivosHoy() {
    String diaHoy = DIA_MAP.get(LocalDate.now().getDayOfWeek());
    LocalTime ahora = LocalTime.now();

    // LOG TEMPORAL - borrarlo después de verificar
    grupoRepository.findAll().forEach(g -> {
        System.out.println(">>> GRUPO: " + g.getNombre() 
            + " | dias: [" + g.getDias() + "]"
            + " | horaInicio: [" + g.getHoraInicio() + "]"
            + " | horaFin: [" + g.getHoraFin() + "]");
    });
    System.out.println(">>> DIA HOY: [" + diaHoy + "] | AHORA: [" + ahora + "]");

    return grupoRepository.findAll().stream()
        .filter(g -> {
            if (g.getDias() == null || diaHoy == null) return false;
            if (!g.getDias().toUpperCase().contains(diaHoy)) return false;

            // Validar que la hora actual esté dentro del horario del grupo
            try {
                if (g.getHoraInicio() == null || g.getHoraFin() == null) return false;
                LocalTime inicio = LocalTime.parse(g.getHoraInicio().trim());
                LocalTime fin    = LocalTime.parse(g.getHoraFin().trim());
                return !ahora.isBefore(inicio) && !ahora.isAfter(fin);
            } catch (Exception e) {
                return false;
            }
        })
        .toList();
}

    private void agregarWidgetCalendario(Model model) {
        YearMonth ym = YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        model.addAttribute("calHoy",          LocalDate.now());
        model.addAttribute("calAnio",          ym.getYear());
        model.addAttribute("calMes",           ym.getMonthValue());
        model.addAttribute("calDiasEnMes",     ym.lengthOfMonth());
        model.addAttribute("calPrimerDia",     inicio.getDayOfWeek().getValue() % 7);
        model.addAttribute("calPeriodoActual", calendarioService.periodoActual().orElse(null));
        model.addAttribute("calDiasEsp",       calendarioService.diasEnRango(inicio, fin));
        model.addAttribute("calEsBloqueado",   calendarioService.esHoyDiaBloqueado());
        model.addAttribute("calMotivoBloqueo", calendarioService.motivoBloqueoHoy());
    }

    // ─────────────────────────────────────────────────────────────
    // VISTA HOY — filtra por grupos activos en este momento
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public String vistaHoy(Model model, Principal principal) {

        Usuario docente = usuarioRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        // Grupos que tienen clase HOY (para el botón flotante)
        List<Grupo> gruposDeAhora = gruposActivosHoy();

        // Todos los grupos (para filtros y selector)
        List<Grupo> todosLosGrupos = grupoRepository.findAll();

        Date hoy = java.sql.Date.valueOf(LocalDate.now());
        LocalTime ahora = LocalTime.now();

        // Asistencias de TODOS los grupos del día
        List<Asistencia> asistenciasRaw = todosLosGrupos.stream()
                .flatMap(g -> asistenciaRepository.findByGrupoAndFecha(g, hoy).stream())
                .collect(Collectors.toList());

        // Auto-ausente: si el horario de la sección ya terminó y el alumno
        // sigue en PENDIENTE, se trata como AUSENTE en la vista (sin tocar BD).
        List<Asistencia> asistencias = asistenciasRaw.stream().map(a -> {
            if (!"PENDIENTE".equals(a.getEstado())) return a;
            Grupo g = a.getGrupo();
            if (g.getHoraFin() == null || g.getHoraFin().isBlank()) return a;
            try {
                LocalTime fin = LocalTime.parse(g.getHoraFin().trim());
                if (ahora.isAfter(fin)) {
                    // Crear copia virtual con estado AUSENTE
                    Asistencia virtual = new Asistencia();
                    virtual.setEstudiante(a.getEstudiante());
                    virtual.setGrupo(a.getGrupo());
                    virtual.setFecha(a.getFecha());
                    virtual.setEstado("AUSENTE");
                    virtual.setHoraEntrada(null);
                    virtual.setHoraSalida(null);
                    return virtual;
                }
            } catch (Exception e) { /* ignorar */ }
            return a;
        }).collect(Collectors.toList());

        List<Asistencia> presentes  = asistencias.stream()
                .filter(a -> "PRESENTE".equals(a.getEstado()) || "TARDANZA".equals(a.getEstado()))
                .toList();
        List<Asistencia> pendientes = asistencias.stream()
                .filter(a -> "PENDIENTE".equals(a.getEstado()))
                .toList();
        List<Asistencia> ausentes   = asistencias.stream()
                .filter(a -> "AUSENTE".equals(a.getEstado()))
                .toList();

        model.addAttribute("docente",           docente);
        model.addAttribute("grupos",            todosLosGrupos);
        model.addAttribute("gruposActivos",     gruposDeAhora);
        model.addAttribute("asistencias",       asistencias);
        model.addAttribute("presentes",         presentes);
        model.addAttribute("pendientes",        pendientes);
        model.addAttribute("ausentes",          ausentes);

        model.addAttribute("fechaConsultada",   hoy);
        model.addAttribute("fechaSeleccionada", LocalDate.now().toString());

        model.addAttribute("modoHistorial",     false);
        model.addAttribute("modoRango",         false);

        model.addAttribute("fechaDesdeStr",     null);
        model.addAttribute("fechaHastaStr",     null);
        model.addAttribute("seccionesSeleccionadas", List.of());

        agregarWidgetCalendario(model);

        return "docente/asistencias";
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORIAL
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/historial")
    public String vistaHistorial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) List<Integer> secciones,
            Model model,
            Principal principal) {

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
                LocalDate tmp = desde; desde = hasta; hasta = tmp;
            }
            desdeStr = desde.toString();
            hastaStr = hasta.toString();

            Date inicio = java.sql.Date.valueOf(desde);
            Date fin    = java.sql.Date.valueOf(hasta);

            List<Grupo> gruposFiltrados = (secciones != null && !secciones.isEmpty())
                    ? grupoRepository.findAllById(secciones)
                    : grupos;

            // Asistencias reales en BD
            List<Asistencia> realesRaw = gruposFiltrados.stream()
                    .flatMap(g -> asistenciaRepository.findByGrupoAndFechaBetween(g, inicio, fin).stream())
                    .collect(Collectors.toList());

            // ── FILTRO DE PERÍODO ──────────────────────────────────────────────
            // Descartar asistencias reales cuya fecha NO pertenece a ningún
            // período escolar activo. Así, registros guardados antes de que
            // existiera un período no aparecen en el historial.
            List<Asistencia> reales = realesRaw.stream()
                    .filter(a -> {
                        if (a.getFecha() == null) return false;
                        LocalDate fechaAsist = ((java.sql.Date) a.getFecha()).toLocalDate();
                        return calendarioService.periodoParaFecha(fechaAsist).isPresent();
                    })
                    .collect(Collectors.toList());
            // ──────────────────────────────────────────────────────────────────

            // Calcular ausentes virtuales: dias que el grupo tenia clase y el estudiante no tiene registro
            List<Asistencia> virtuales = generarAusentesVirtuales(gruposFiltrados, desde, hasta, reales);

            List<Asistencia> todos = new ArrayList<>(reales);
            todos.addAll(virtuales);
            asistencias = todos;

        } else {
            asistencias = List.of();
        }

        model.addAttribute("docente",       docente);
        model.addAttribute("grupos",        grupos);
        model.addAttribute("asistencias",   asistencias);
        model.addAttribute("presentes",     List.of());
        model.addAttribute("pendientes",    List.of());
        model.addAttribute("gruposActivos", List.of());

        model.addAttribute("modoHistorial", true);
        model.addAttribute("modoRango",     modoRango);

        model.addAttribute("fechaDesdeStr", desdeStr);
        model.addAttribute("fechaHastaStr", hastaStr);
        model.addAttribute("fechaSeleccionada", desdeStr);
        model.addAttribute("fechaConsultada",
                desde != null ? java.sql.Date.valueOf(desde) : java.sql.Date.valueOf(LocalDate.now()));
        model.addAttribute("seccionesSeleccionadas",
                secciones != null ? secciones : List.of());

        agregarWidgetCalendario(model);

        return "docente/asistencias";
    }

    // ─────────────────────────────────────────────────────────────
    // EXPORTAR
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/exportar")
    public void exportar(
            @RequestParam String filtro,
            @RequestParam String formato,
            @RequestParam(required = false) List<Integer> secciones,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String estadoFiltro,
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response) throws Exception {

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy, fin = hoy;

        switch (filtro) {
            case "rango", "alumno" -> {
                inicio = desde != null ? desde : hoy;
                fin    = hasta != null ? hasta : hoy;
                if (fin.isBefore(inicio)) { LocalDate t = inicio; inicio = fin; fin = t; }
            }
            default -> {
                inicio = fecha != null ? fecha : hoy;
                fin    = inicio;
            }
        }

        Date dInicio = java.sql.Date.valueOf(inicio);
        Date dFin    = java.sql.Date.valueOf(fin);

        // Estados permitidos según el filtro de vista (ej: "PRESENTE,TARDANZA" o "AUSENTE")
        final List<String> estadosPermitidos = (estadoFiltro != null && !estadoFiltro.isBlank())
                ? List.of(estadoFiltro.split(","))
                : List.of();

        List<Asistencia> asistencias;
        String etiqueta;

        if (idEstudiante != null) {
            asistencias = asistenciaRepository
                    .findByEstudiante_IdEstudiante(idEstudiante).stream()
                    .filter(a -> a.getFecha() != null
                              && !a.getFecha().before(dInicio)
                              && !a.getFecha().after(dFin))
                    .toList();
            etiqueta = "alumno-" + idEstudiante;

        } else if (grupo != null && !grupo.isBlank()) {
            asistencias = grupoRepository.findByNombre(grupo)
                    .map(g -> asistenciaRepository.findByGrupoAndFechaBetween(g, dInicio, dFin))
                    .orElse(List.of());
            etiqueta = "grupo-" + grupo;

        } else if (secciones != null && !secciones.isEmpty()) {
            asistencias = grupoRepository.findAllById(secciones).stream()
                    .flatMap(g -> asistenciaRepository.findByGrupoAndFechaBetween(g, dInicio, dFin).stream())
                    .toList();
            etiqueta = filtro;

        } else {
            asistencias = grupoRepository.findAll().stream()
                    .flatMap(g -> asistenciaRepository.findByGrupoAndFechaBetween(g, dInicio, dFin).stream())
                    .toList();
            etiqueta = filtro;
        }

        // Aplicar filtro de estado si viene de la vista Hoy
        if (!estadosPermitidos.isEmpty()) {
            asistencias = asistencias.stream()
                    .filter(a -> estadosPermitidos.contains(a.getEstado()))
                    .toList();
        }

        if ("excel".equals(formato)) {
            exportService.exportarAsistenciasExcel(asistencias, etiqueta, inicio.toString(), fin.toString(), response);
        } else {
            exportService.exportarAsistenciasPDF(asistencias, etiqueta, inicio.toString(), fin.toString(), response);
        }
    }
    // ─────────────────────────────────────────────────────────────
    // HELPER: genera registros AUSENTE virtuales para el historial
    // Para cada grupo, cada dia del rango en que tiene clase,
    // si no existe registro real del estudiante → se crea un objeto
    // Asistencia en memoria (sin guardar en BD) con estado AUSENTE.
    // ─────────────────────────────────────────────────────────────
    private List<Asistencia> generarAusentesVirtuales(
            List<Grupo> grupos,
            LocalDate desde,
            LocalDate hasta,
            List<Asistencia> reales) {

        // Índice de registros reales: grupoId + estudianteId + fecha → Asistencia
        Set<String> claves = reales.stream()
                .map(a -> a.getGrupo().getIdGrupo() + "_"
                        + a.getEstudiante().getIdEstudiante() + "_"
                        + a.getFecha().toString())
                .collect(Collectors.toSet());

        List<Asistencia> virtuales = new ArrayList<>();

        for (Grupo grupo : grupos) {
            if (grupo.getEstudiantes() == null || grupo.getEstudiantes().isEmpty()) continue;
            String diasGrupo = grupo.getDias() != null ? grupo.getDias().toUpperCase() : "";

            LocalDate dia = desde;
            while (!dia.isAfter(hasta)) {
                // ── VALIDACIÓN DE PERÍODO ──────────────────────────────────────
                // Solo generar ausentes virtuales para fechas que pertenecen
                // a un período escolar activo. Si el día cae fuera de todo
                // período, se omite completamente (no se muestra como ausencia).
                if (calendarioService.periodoParaFecha(dia).isEmpty()) {
                    dia = dia.plusDays(1);
                    continue;
                }
                // ──────────────────────────────────────────────────────────────

                String diaStr = DIA_MAP.get(dia.getDayOfWeek());
                if (diaStr != null && diasGrupo.contains(diaStr)) {
                    Date fecha = java.sql.Date.valueOf(dia);
                    String fechaStr = fecha.toString();

                    for (AsistenciaFGK.Oportunidades.models.Estudiante est : grupo.getEstudiantes()) {
                        String clave = grupo.getIdGrupo() + "_" + est.getIdEstudiante() + "_" + fechaStr;
                        if (!claves.contains(clave)) {
                            // No hay registro real → ausente virtual
                            Asistencia virtual = new Asistencia();
                            virtual.setEstudiante(est);
                            virtual.setGrupo(grupo);
                            virtual.setFecha(fecha);
                            virtual.setEstado("AUSENTE");
                            virtual.setHoraEntrada(null);
                            virtual.setHoraSalida(null);
                            virtuales.add(virtual);
                        }
                    }
                }
                dia = dia.plusDays(1);
            }
        }
        return virtuales;
    }

}
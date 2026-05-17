package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.*;
import AsistenciaFGK.Oportunidades.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SupervisorService {

    @Autowired
    private AsistenciaRepository asistenciaRepo;

    @Autowired
    private GrupoRepository grupoRepo;

    @Autowired
    private EstudianteRepository estudianteRepo;

    @Autowired
    private CalendarioService calendarioService;

    // ── 1. Estadísticas globales ───────────────────────────────
    public Map<String, Object> obtenerEstadisticasGlobales() {
        Map<String, Object> stats = new HashMap<>();

        List<Asistencia> todas = asistenciaRepo.findAll();
        long total = todas.size();
        long presentes = todas.stream().filter(a -> "PRESENTE".equals(a.getEstado())).count();
        long ausentes  = todas.stream().filter(a -> "AUSENTE".equals(a.getEstado())).count();
        long tardanzas = todas.stream().filter(a -> "TARDANZA".equals(a.getEstado())).count();

        stats.put("total", total);
        stats.put("presentes", presentes);
        stats.put("ausentes", ausentes);
        stats.put("tardanzas", tardanzas);
        stats.put("pctPresente", total > 0 ? Math.round((presentes * 100.0) / total) : 0);
        stats.put("pctAusente",  total > 0 ? Math.round((ausentes  * 100.0) / total) : 0);
        stats.put("pctTardanza", total > 0 ? Math.round((tardanzas * 100.0) / total) : 0);

        return stats;
    }

    // ── 2. Estadísticas por grupo ──────────────────────────────
    public List<Map<String, Object>> obtenerEstadisticasPorGrupo() {
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Grupo grupo : grupoRepo.findAll()) {
            Map<String, Object> item = new HashMap<>();
            long presentes = asistenciaRepo.countByGrupoAndEstado(grupo, "PRESENTE");
            long ausentes  = asistenciaRepo.countByGrupoAndEstado(grupo, "AUSENTE");
            long tardanzas = asistenciaRepo.countByGrupoAndEstado(grupo, "TARDANZA");
            long total = presentes + ausentes + tardanzas;

            item.put("grupo", grupo.getNombre());
            item.put("presentes", presentes);
            item.put("ausentes", ausentes);
            item.put("tardanzas", tardanzas);
            item.put("total", total);
            item.put("pct", total > 0 ? Math.round((presentes * 100.0) / total) : 0);
            resultado.add(item);
        }
        return resultado;
    }

    // ── 3. Detectar estudiantes en riesgo (por tardanzas en período) ──
public List<Map<String, Object>> detectarRiesgoAusentismo() {
    List<Map<String, Object>> enRiesgo = new ArrayList<>();

    // Obtener período activo; si no hay, usar las últimas 10 semanas
    java.time.LocalDate fechaInicio = null;
    java.time.LocalDate fechaFin = null;
    
    var periodoOpt = calendarioService.periodoActual();
    if (periodoOpt.isPresent()) {
        fechaInicio = periodoOpt.get().getFechaInicio();
        fechaFin = periodoOpt.get().getFechaFin();
    } else {
        // Fallback: últimas 10 semanas
        fechaFin = java.time.LocalDate.now();
        fechaInicio = fechaFin.minusWeeks(10);
    }

    final java.time.LocalDate inicio = fechaInicio;
    final java.time.LocalDate fin = fechaFin;

    for (Estudiante est : estudianteRepo.findAll()) {
        // Convertir LocalDate a java.util.Date para el repo
        java.util.Date dateInicio = java.sql.Date.valueOf(inicio);
        java.util.Date dateFin = java.sql.Date.valueOf(fin);

        List<Asistencia> asistenciasPeriodo = asistenciaRepo
            .findByEstudiante_IdEstudianteAndFechaBetween(
                est.getIdEstudiante(), dateInicio, dateFin);

        long ausencias = asistenciasPeriodo.stream()
            .filter(a -> "AUSENTE".equals(a.getEstado())).count();

        long tardanzas = asistenciasPeriodo.stream()
            .filter(a -> "TARDANZA".equals(a.getEstado())).count();

        long total = asistenciasPeriodo.size();

        // Umbrales por TARDANZAS: 2=PRECAUCION, 3=ALERTA, 4+=CRITICO
        if (tardanzas >= 2) {
            String nivel;
            if (tardanzas >= 4) {
                nivel = "CRITICO";
            } else if (tardanzas == 3) {
                nivel = "ALERTA";
            } else {
                nivel = "PRECAUCION";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("nombre", est.getNombre() + " " + est.getApellido());
            item.put("ausencias", ausencias);
            item.put("tardanzas", tardanzas);
            item.put("total", total);
            item.put("nivel", nivel);
            enRiesgo.add(item);
        }
    }

    enRiesgo.sort((a, b) ->
        Long.compare((Long) b.get("tardanzas"), (Long) a.get("tardanzas")));

    return enRiesgo;
}

    // ── 4. Todas las asistencias (para vista general) ─────────
    public List<Asistencia> listarTodasAsistencias() {
        return asistenciaRepo.findAll();
    }

    public List<Grupo> listarGrupos() {
        return grupoRepo.findAll();        
    }
    
    public List<Asistencia> listarAsistenciasPorFecha(java.time.LocalDate fecha) {
    java.util.Date fechaDate = java.sql.Date.valueOf(fecha);
    return grupoRepo.findAll().stream()
        .flatMap(g -> asistenciaRepo.findByGrupoAndFecha(g, fechaDate).stream())
        .toList();
}
}
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

    // ── 1. Estadísticas globales (acotadas al período activo) ──────────────
    public Map<String, Object> obtenerEstadisticasGlobales() {
        Map<String, Object> stats = new HashMap<>();

        // Obtener rango del período activo; si no hay período, no hay datos
        var periodoOpt = calendarioService.periodoActual();
        List<Asistencia> todas;
        if (periodoOpt.isPresent()) {
            java.util.Date dInicio = java.sql.Date.valueOf(periodoOpt.get().getFechaInicio());
            java.util.Date dFin    = java.sql.Date.valueOf(periodoOpt.get().getFechaFin());
            todas = asistenciaRepo.findByFechaBetween(dInicio, dFin);
        } else {
            todas = List.of();
        }

        long total     = todas.size();
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

    // ── 2. Estadísticas por grupo (acotadas al período activo) ───────────
    public List<Map<String, Object>> obtenerEstadisticasPorGrupo() {
        List<Map<String, Object>> resultado = new ArrayList<>();

        // Obtener rango del período activo; si no hay período, devolver lista vacía
        var periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) return resultado;

        java.util.Date dInicio = java.sql.Date.valueOf(periodoOpt.get().getFechaInicio());
        java.util.Date dFin    = java.sql.Date.valueOf(periodoOpt.get().getFechaFin());

        for (Grupo grupo : grupoRepo.findAll()) {
            Map<String, Object> item = new HashMap<>();
            long presentes = asistenciaRepo.countByGrupoAndEstadoAndFechaBetween(grupo, "PRESENTE", dInicio, dFin);
            long ausentes  = asistenciaRepo.countByGrupoAndEstadoAndFechaBetween(grupo, "AUSENTE",  dInicio, dFin);
            long tardanzas = asistenciaRepo.countByGrupoAndEstadoAndFechaBetween(grupo, "TARDANZA", dInicio, dFin);
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

    // ── 3. Detectar estudiantes en riesgo (por AUSENCIAS en período activo) ──
    public List<Map<String, Object>> detectarRiesgoAusentismo() {
        List<Map<String, Object>> enRiesgo = new ArrayList<>();

        // Si no hay período activo, la vista de riesgo debe estar vacía.
        // No se usa fallback para evitar mostrar datos de períodos anteriores.
        var periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) return enRiesgo;

        java.util.Date dateInicio = java.sql.Date.valueOf(periodoOpt.get().getFechaInicio());
        java.util.Date dateFin    = java.sql.Date.valueOf(periodoOpt.get().getFechaFin());

        for (Estudiante est : estudianteRepo.findAll()) {
            List<Asistencia> asistenciasPeriodo = asistenciaRepo
                .findByEstudiante_IdEstudianteAndFechaBetween(
                    est.getIdEstudiante(), dateInicio, dateFin);

            long ausencias = asistenciasPeriodo.stream()
                .filter(a -> "AUSENTE".equals(a.getEstado())).count();

            long tardanzas = asistenciasPeriodo.stream()
                .filter(a -> "TARDANZA".equals(a.getEstado())).count();

            long total = asistenciasPeriodo.size();

                // DESPUÉS
          if (ausencias >= 3) {
              String nivel;
              if (ausencias >= 10) {
                  nivel = "CRITICO";
              } else {
                  nivel = "PRECAUCION";
              }

                Map<String, Object> item = new HashMap<>();
                item.put("idEstudiante", est.getIdEstudiante());
                item.put("nombre", est.getNombre() + " " + est.getApellido());
                item.put("ausencias", ausencias);
                item.put("tardanzas", tardanzas);
                item.put("total", total);
                item.put("nivel", nivel);
                enRiesgo.add(item);
            }
        }

        enRiesgo.sort((a, b) ->
            Long.compare((Long) b.get("ausencias"), (Long) a.get("ausencias")));

        return enRiesgo;
    }

    // ── 4. Todas las asistencias acotadas al período activo ───────────────
    public List<Asistencia> listarTodasAsistencias() {
        var periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) return List.of();
        java.util.Date dInicio = java.sql.Date.valueOf(periodoOpt.get().getFechaInicio());
        java.util.Date dFin    = java.sql.Date.valueOf(periodoOpt.get().getFechaFin());
        return asistenciaRepo.findByFechaBetween(dInicio, dFin);
    }

    public List<Grupo> listarGrupos() {
        return grupoRepo.findAll();        
    }
    
    public List<Asistencia> listarAsistenciasPorFecha(java.time.LocalDate fecha) {
        // Si la fecha no pertenece a ningún período activo, no mostrar asistencias
        if (calendarioService.periodoParaFecha(fecha).isEmpty()) {
            return java.util.List.of();
        }
        java.util.Date fechaDate = java.sql.Date.valueOf(fecha);
        return grupoRepo.findAll().stream()
            .flatMap(g -> asistenciaRepo.findByGrupoAndFecha(g, fechaDate).stream())
            .toList();
    }
}
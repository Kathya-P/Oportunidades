package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.models.Asistencia;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import AsistenciaFGK.Oportunidades.services.CalendarioService; // ← NUEVO
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/asistencia")
public class AsistenciaController {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private CalendarioService calendarioService; // ← NUEVO

    // ── Página del lector ─────────────────────────────────────────────────────
    @GetMapping("/lector")
    public String paginaLector(Model model) {
        // ── NUEVO: avisar si hoy está bloqueado ──────────────────────────────
        boolean bloqueado = calendarioService.esHoyDiaBloqueado();
        model.addAttribute("diaBloqueado", bloqueado);
        model.addAttribute("motivoBloqueo", calendarioService.motivoBloqueoHoy());
        // ────────────────────────────────────────────────────────────────────
        model.addAttribute("ultimo", null);
        model.addAttribute("error",  null);
        return "asistencia/lector";
    }

    // ── Endpoint que recibe el escaneo ────────────────────────────────────────
    @PostMapping("/registrar")
    public String registrar(@RequestParam("codigoBarras") String codigoBarras,
                            Model model) {

        codigoBarras = codigoBarras.trim();

        // ── NUEVO: bloquear si es festivo / asueto / semana pedagógica ────────
        if (calendarioService.esHoyDiaBloqueado()) {
            String motivo = calendarioService.motivoBloqueoHoy();
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "bloqueado");
            model.addAttribute("diaBloqueado", true);
            model.addAttribute("motivoBloqueo", motivo);
            model.addAttribute("error",
                "Hoy es un día no lectivo (" + motivo + "). No se puede registrar asistencia.");
            return "asistencia/lector";
        }
        // ────────────────────────────────────────────────────────────────────

        Optional<Estudiante> opt = estudianteRepository.findByCodigoBarras(codigoBarras);

        if (opt.isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error", "Código no encontrado: " + codigoBarras);
            return "asistencia/lector";
        }

        Estudiante estudiante = opt.get();

        // ── Validación por jornada: SABATINO solo puede marcar en sábado ───
        LocalDate hoyLocal = LocalDate.now();
        if (estudiante.getJornada() == Estudiante.Jornada.SABATINO
                && hoyLocal.getDayOfWeek() != DayOfWeek.SATURDAY) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error",
                "Este alumno es SABATINO. Solo puede registrar asistencia en sábado.");
            return "asistencia/lector";
        }

        Date hoy = java.sql.Date.valueOf(java.time.LocalDate.now());
        List<Grupo> grupos = estudiante.getGrupos();

        if (grupos == null || grupos.isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error",
                estudiante.getNombre() + " no está asignado a ningún grupo.");
            return "asistencia/lector";
        }

        String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellido();
        String horaActual = java.time.LocalTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        String estadoResultado = "entrada";

        for (Grupo grupo : grupos) {
            List<Asistencia> registrosHoy = asistenciaRepository
                .findByGrupoAndFecha(grupo, hoy)
                .stream()
                .filter(a -> a.getEstudiante().getIdEstudiante()
                              .equals(estudiante.getIdEstudiante()))
                .collect(java.util.stream.Collectors.toList());

            if (registrosHoy.isEmpty()) {
                String estadoAsistencia = "PRESENTE";
                try {
                    String horarioStr  = grupo.getHorario();
                    String horasParte  = horarioStr.substring(horarioStr.lastIndexOf(" ") + 1);
                    String horaInicioStr = horasParte.split("-")[0];

                    java.time.LocalTime horaInicio  = java.time.LocalTime.parse(horaInicioStr);
                    java.time.LocalTime horaEntrada = java.time.LocalTime.now();

                    if (horaEntrada.isAfter(horaInicio.plusMinutes(5))) {
                        estadoAsistencia = "TARDANZA";
                        estadoResultado  = "tardanza";
                    } else {
                        estadoResultado  = "entrada";
                    }
                } catch (Exception e) {
                    estadoResultado = "entrada";
                }

                Asistencia asistencia = new Asistencia();
                asistencia.setEstudiante(estudiante);
                asistencia.setGrupo(grupo);
                asistencia.setFecha(hoy);
                asistencia.setEstado(estadoAsistencia);
                asistencia.setHoraEntrada(horaActual);
                asistenciaRepository.save(asistencia);

            } else {
                Asistencia existente = registrosHoy.get(0);
                if (existente.getHoraSalida() == null) {
                    existente.setHoraSalida(horaActual);
                    asistenciaRepository.save(existente);
                    estadoResultado = "salida";
                } else {
                    estadoResultado = "completo";
                }
            }
        }

        model.addAttribute("ultimo", nombreCompleto);
        model.addAttribute("estado", estadoResultado);
        model.addAttribute("diaBloqueado", false);
        model.addAttribute("error", null);
        return "asistencia/lector";
    }
}
package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.models.Asistencia;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import AsistenciaFGK.Oportunidades.services.CalendarioService;
import AsistenciaFGK.Oportunidades.services.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private CalendarioService calendarioService;

    // Mapeo DayOfWeek → nombre en BD
    private static final Map<DayOfWeek, String> MAPA_DIAS = Map.of(
        DayOfWeek.MONDAY,    "LUNES",
        DayOfWeek.TUESDAY,   "MARTES",
        DayOfWeek.WEDNESDAY, "MIERCOLES",
        DayOfWeek.THURSDAY,  "JUEVES",
        DayOfWeek.FRIDAY,    "VIERNES",
        DayOfWeek.SATURDAY,  "SABADO",
        DayOfWeek.SUNDAY,    "DOMINGO"
    );

    // ── Página del lector ─────────────────────────────────────────────────────
    @GetMapping("/lector")
    public String paginaLector(Model model) {
        model.addAttribute("diaBloqueado", calendarioService.esHoyDiaBloqueado());
        model.addAttribute("motivoBloqueo", calendarioService.motivoBloqueoHoy());
        model.addAttribute("ultimo", null);
        model.addAttribute("error",  null);
        return "asistencia/lector";
    }


    // ── Endpoint que recibe el escaneo ────────────────────────────────────────
    @PostMapping("/registrar")
    public String registrar(@RequestParam("codigoBarras") String codigoBarras,
                            Model model) {

        codigoBarras = codigoBarras.trim();

        // ── Bloquear si no hay período escolar activo ────────────────────────
        if (calendarioService.periodoActual().isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "bloqueado");
            model.addAttribute("diaBloqueado", true);
            model.addAttribute("motivoBloqueo", "Sin período asignado");
            model.addAttribute("error",
                "No hay ningún período escolar activo. No se puede registrar asistencia.");
            return "asistencia/lector";
        }

        // ── Bloquear si es festivo / asueto / semana pedagógica ───────────────
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

        // ── Buscar estudiante ─────────────────────────────────────────────────
        Optional<Estudiante> opt = estudianteRepository.findByCodigoBarras(codigoBarras);

        if (opt.isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error", "Código no encontrado: " + codigoBarras);
            return "asistencia/lector";
        }

        Estudiante estudiante = opt.get();
        LocalDate  hoyLocal   = LocalDate.now();
        DayOfWeek  diaSemana  = hoyLocal.getDayOfWeek();
        String     diaHoy     = MAPA_DIAS.get(diaSemana);   // ej: "LUNES"
        Date       hoy        = java.sql.Date.valueOf(hoyLocal);

        // ── Verificar que tenga grupos ────────────────────────────────────────
        List<Grupo> grupos = estudiante.getGrupos();

        if (grupos == null || grupos.isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error",
                estudiante.getNombre() + " no está asignado a ningún grupo.");
            return "asistencia/lector";
        }

        // ── Filtrar solo los grupos que tienen clase hoy ──────────────────────
        List<Grupo> gruposHoy = grupos.stream()
            .filter(g -> {
                String dias = g.getDias() != null ? g.getDias() : "";
                return dias.contains(diaHoy);
            })
            .collect(java.util.stream.Collectors.toList());

        if (gruposHoy.isEmpty()) {
            model.addAttribute("ultimo", null);
            model.addAttribute("estado", "error");
            model.addAttribute("diaBloqueado", false);
            model.addAttribute("error",
                estudiante.getNombre() + " no tiene clase hoy (" + diaHoy + ").");
            return "asistencia/lector";
        }

        // ── Registrar asistencia en cada grupo con clase hoy ──────────────────
        String nombreCompleto = estudiante.getNombre() + " " + estudiante.getApellido();
        LocalTime ahoraLocal = LocalTime.now(ZoneId.of("America/El_Salvador"));
        String horaActual     = ahoraLocal.format(DateTimeFormatter.ofPattern("HH:mm"));
        String estadoResultado = "entrada";

        for (Grupo grupo : gruposHoy) {

            List<Asistencia> registrosHoy = asistenciaRepository
                .findByGrupoAndFecha(grupo, hoy)
                .stream()
                .filter(a -> a.getEstudiante().getIdEstudiante()
                              .equals(estudiante.getIdEstudiante()))
                .collect(java.util.stream.Collectors.toList());

            // ── Si existe un registro PENDIENTE (creado por scheduler), se completa como primera marca ──
            Asistencia existente = registrosHoy.isEmpty() ? null : registrosHoy.get(0);
            boolean esPendienteSinEntrada = existente != null
                    && "PENDIENTE".equalsIgnoreCase(existente.getEstado())
                    && (existente.getHoraEntrada() == null || existente.getHoraEntrada().isBlank());

            if (registrosHoy.isEmpty() || esPendienteSinEntrada) {
                // ── Primera marca del día → determinar estado ─────────────────
                String estadoAsistencia = "PRESENTE";

                try {
                    LocalTime horaEntrada = LocalTime.now(ZoneId.of("America/El_Salvador"));

                    // ¿Llegó después del fin? → AUSENTE
                    //if (grupo.getHoraFin() != null && !grupo.getHoraFin().isBlank()) {
                   //     LocalTime fin = LocalTime.parse(grupo.getHoraFin());
                     //   if (horaEntrada.isAfter(fin)) {
                       //     estadoAsistencia = "AUSENTE";
                         //   estadoResultado  = "ausente";
                        //}
                    //}

                    // ¿Llegó tarde pero antes del fin? → TARDANZA
                    if (!"AUSENTE".equals(estadoAsistencia)
                            && grupo.getHoraInicio() != null
                            && !grupo.getHoraInicio().isBlank()) {
                        LocalTime inicio = LocalTime.parse(grupo.getHoraInicio());
                        if (horaEntrada.isAfter(inicio.plusMinutes(5))) {
                            estadoAsistencia = "TARDANZA";
                            estadoResultado  = "tardanza";
                        } else {
                            estadoResultado  = "entrada";
                        }
                    }

                } catch (Exception e) {
                    estadoResultado = "entrada";
                }

                Asistencia asistencia = (existente != null) ? existente : new Asistencia();
                asistencia.setEstudiante(estudiante);
                asistencia.setGrupo(grupo);
                asistencia.setFecha(hoy);
                asistencia.setEstado(estadoAsistencia);
                asistencia.setHoraEntrada(horaActual);
                asistenciaRepository.save(asistencia);

            } else {
                // ── Segunda marca → registrar salida ──────────────────────────
                if (existente.getHoraSalida() == null) {
                    // Si todavía estamos en la ventana de entrada del grupo
                    // (antes o muy cerca de la horaInicio), no registrar salida.
                    try {
                        if (grupo.getHoraInicio() != null && !grupo.getHoraInicio().isBlank()) {
                            LocalTime inicio = LocalTime.parse(grupo.getHoraInicio().trim());
                            if (!ahoraLocal.isAfter(inicio.plusMinutes(5))) {
                                // Mantener como entrada (evita que un escaneo temprano convierta el de las 8 en salida)
                                estadoResultado = "entrada";
                                continue;
                            }
                        }
                    } catch (Exception ignore) {
                        // Si hay un formato de hora inválido, dejamos el comportamiento por defecto
                    }

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
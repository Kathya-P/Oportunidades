package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.Alerta;
import AsistenciaFGK.Oportunidades.models.Asistencia;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.models.PeriodoEscolar;
import AsistenciaFGK.Oportunidades.models.Role;
import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.repositories.AlertaRepository;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import AsistenciaFGK.Oportunidades.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AlertaScheduler {

    // ── TODAS las inyecciones juntas arriba (evita NullPointerException) ──
    @Autowired private EstudianteRepository estudianteRepo;
    @Autowired private AsistenciaRepository asistenciaRepo;
    @Autowired private AlertaRepository alertaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private GrupoRepository grupoRepo;
    @Autowired private CalendarioService calendarioService;
    @Autowired private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PENDIENTES → AUSENTES + notificación al supervisor
    //    Corre cada 5 minutos.
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void actualizarPendientesYAusentesPorHorario() {

        LocalDate hoyLocal = LocalDate.now();

        Optional<PeriodoEscolar> periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) return;

        if (calendarioService.esHoyDiaBloqueado()) return;

        String diaHoy = Map.of(
            DayOfWeek.MONDAY,    "LUNES",
            DayOfWeek.TUESDAY,   "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY,  "JUEVES",
            DayOfWeek.FRIDAY,    "VIERNES",
            DayOfWeek.SATURDAY,  "SABADO",
            DayOfWeek.SUNDAY,    "DOMINGO"
        ).get(hoyLocal.getDayOfWeek());

        LocalTime ahora = LocalTime.now(ZoneId.of("America/El_Salvador"));
        Date hoy = java.sql.Date.valueOf(hoyLocal);

        List<Usuario> supervisores = usuarioRepo.findByRole(Role.ROLE_SUPERVISOR);

        // Acumulado global de faltas (una sola notificación por supervisor)
        Map<String, List<String>> ausentesPorSeccion = new LinkedHashMap<>();

        for (Grupo grupo : grupoRepo.findAll()) {

            // ¿Tiene clase hoy?
            String dias = grupo.getDias() != null ? grupo.getDias().toUpperCase() : "";
            if (!dias.contains(diaHoy)) continue;

            // Requiere horario configurado
            if (grupo.getHoraInicio() == null || grupo.getHoraInicio().isBlank()
                    || grupo.getHoraFin() == null || grupo.getHoraFin().isBlank()) continue;

            LocalTime inicio;
            LocalTime fin;
            try {
                inicio = LocalTime.parse(grupo.getHoraInicio().trim());
                fin    = LocalTime.parse(grupo.getHoraFin().trim());
            } catch (Exception e) {
                continue;
            }

            // Antes de iniciar la clase no marcar nada
            if (ahora.isBefore(inicio)) continue;

            boolean duranteClase = !ahora.isAfter(fin);

            // Cuando la clase ya terminó: acumulamos ausentes nuevos y enviamos un solo correo
            // con todos los que faltaron (en vez de uno por alumno).
            List<String> ausentesNuevos = duranteClase ? List.of() : new ArrayList<>();

            List<Estudiante> inscritos = grupo.getEstudiantes();
            if (inscritos == null || inscritos.isEmpty()) continue;

            var registrosHoy = asistenciaRepo.findByGrupoAndFecha(grupo, hoy);
            var registroPorEstudiante = registrosHoy.stream()
                    .collect(Collectors.toMap(
                            a -> a.getEstudiante().getIdEstudiante(),
                            a -> a,
                            (a1, a2) -> a1
                    ));

            for (Estudiante est : inscritos) {

                // Filtro de modalidad/jornada
                String modalidadGrupo = grupo.getModalidad();
                if (modalidadGrupo != null && !modalidadGrupo.isBlank()) {
                    if (!modalidadGrupo.trim().equalsIgnoreCase(est.getJornada().name())) continue;
                }

                Asistencia existente = registroPorEstudiante.get(est.getIdEstudiante());

                if (existente == null) {
                    // Sin registro todavía
                    Asistencia nuevo = new Asistencia();
                    nuevo.setEstudiante(est);
                    nuevo.setGrupo(grupo);
                    nuevo.setFecha(hoy);
                    nuevo.setHoraEntrada(null);
                    nuevo.setHoraSalida(null);
                    nuevo.setEstado(duranteClase ? "PENDIENTE" : "AUSENTE");
                    asistenciaRepo.save(nuevo);

                    if (!duranteClase) {
                        ausentesNuevos.add(est.getNombre() + " " + est.getApellido());
                    }

                } else {
                    // Con registro: si terminó la clase y estaba PENDIENTE → AUSENTE
                    if (!duranteClase && "PENDIENTE".equalsIgnoreCase(existente.getEstado())) {
                        existente.setEstado("AUSENTE");
                        asistenciaRepo.save(existente);
                        ausentesNuevos.add(est.getNombre() + " " + est.getApellido());
                    }
                }
            }

            if (!duranteClase && !ausentesNuevos.isEmpty()) {
                StringBuilder etiqueta = new StringBuilder();
                etiqueta.append(grupo.getNombre() != null ? grupo.getNombre() : "(Sin nombre)");

                String diasEtiq = (grupo.getDias() != null && !grupo.getDias().isBlank())
                        ? grupo.getDias().replace(",", " · ")
                        : null;
                String horasEtiq = (grupo.getHoraInicio() != null && !grupo.getHoraInicio().isBlank()
                        && grupo.getHoraFin() != null && !grupo.getHoraFin().isBlank())
                        ? grupo.getHoraInicio() + " - " + grupo.getHoraFin()
                        : null;

                if (diasEtiq != null || horasEtiq != null) {
                    etiqueta.append(" — ");
                    if (diasEtiq != null) etiqueta.append(diasEtiq);
                    if (diasEtiq != null && horasEtiq != null) etiqueta.append(" ");
                    if (horasEtiq != null) etiqueta.append("(").append(horasEtiq).append(")");
                }

                ausentesPorSeccion.put(etiqueta.toString(), ausentesNuevos);
            }
        }

        if (!ausentesPorSeccion.isEmpty()) {
            String fechaStr = hoyLocal.toString();
            for (Usuario sup : supervisores) {
                if (sup.getEmail() == null || sup.getEmail().isBlank()) continue;
                try {
                    emailService.enviarFaltasDelDiaResumen(sup.getEmail(), fechaStr, ausentesPorSeccion);
                } catch (Exception e) {
                    System.err.println("[Scheduler] Error enviando correo resumen de faltas a " + sup.getEmail() + ": " + e.getMessage());
                }
            }

            int totalAusentes = ausentesPorSeccion.values().stream().mapToInt(List::size).sum();
            System.out.println("[Scheduler] Resumen de faltas enviado (secciones=" + ausentesPorSeccion.size() + ", ausentes=" + totalAusentes + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. AUSENTISMO ACUMULADO (>=3 ausencias en el período)
    //    Corre cada 1 minuto.
    // ─────────────────────────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 60_000)
    public void verificarAusentismo() {

        System.out.println("[Scheduler] Verificando ausentismo acumulado...");

        Optional<PeriodoEscolar> periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) {
            System.out.println("[Scheduler] Sin período activo; no se envían alertas.");
            return;
        }

        PeriodoEscolar periodo = periodoOpt.get();
        Date periodoInicio = java.sql.Date.valueOf(periodo.getFechaInicio());
        Date periodoFin    = java.sql.Date.valueOf(periodo.getFechaFin());

        List<Usuario> supervisores = usuarioRepo.findByRole(Role.ROLE_SUPERVISOR);

        for (Estudiante est : estudianteRepo.findAll()) {

            long ausencias = asistenciaRepo
                .findByEstudiante_IdEstudianteAndFechaBetween(
                        est.getIdEstudiante(), periodoInicio, periodoFin)
                .stream()
                .filter(a -> "AUSENTE".equals(a.getEstado()))
                .count();

        String nivel;
                // DESPUÉS
        if (ausencias >= 10) {
            nivel = "CRITICO";
        } else if (ausencias >= 3) {
            nivel = "PRECAUCION";
        } else {
            continue;
        }

        boolean yaAlertado = alertaRepo.existsByEstudianteAndTipoAndAtendidaAndFechaAlertaBetween(
                est, "INASISTENCIA", false, periodoInicio, periodoFin);

        if (!yaAlertado) {
            Alerta alerta = new Alerta();
            alerta.setEstudiante(est);
            alerta.setTipo("INASISTENCIA");
            alerta.setFechaAlerta(new Date());
            alerta.setAtendida(false);
            alerta.setDescripcion(
                "Estudiante tiene " + ausencias + " ausencias en el período: " + periodo.getNombre());
            alertaRepo.save(alerta);

            for (Usuario sup : supervisores) {
                if (sup.getEmail() != null && !sup.getEmail().isBlank()) {
                    emailService.enviarAlertaAusentismo(
                        sup.getEmail(),
                        est.getNombre() + " " + est.getApellido(),
                        ausencias,
                        nivel
                    );
                }
            }
            System.out.println("[Scheduler] Alerta " + nivel + " enviada para: " + est.getNombre());
        }
        }
    }

}
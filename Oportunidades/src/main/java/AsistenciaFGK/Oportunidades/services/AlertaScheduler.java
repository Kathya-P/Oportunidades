package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.Alerta;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.PeriodoEscolar;
import AsistenciaFGK.Oportunidades.models.Role;
import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.repositories.AlertaRepository;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.repositories.UsuarioRepository;
import AsistenciaFGK.Oportunidades.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalTime;

@Component
public class AlertaScheduler {

    @Autowired private EstudianteRepository estudianteRepo;
    @Autowired private AsistenciaRepository asistenciaRepo;
    @Autowired private AlertaRepository alertaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private EmailService emailService;

    // Corre periódicamente. Ajustá el delay según necesidad.
    @Scheduled(fixedDelay = 60000)
    public void verificarAusentismo() {

        System.out.println("[Scheduler] Verificando ausentismo...");

        Optional<PeriodoEscolar> periodoOpt = calendarioService.periodoActual();
        if (periodoOpt.isEmpty()) {
            System.out.println("[Scheduler] Sin período activo; no se envían alertas.");
            return;
        }

        PeriodoEscolar periodo = periodoOpt.get();
        Date periodoInicio = java.sql.Date.valueOf(periodo.getFechaInicio());
        Date periodoFin = java.sql.Date.valueOf(periodo.getFechaFin());

        List<Usuario> supervisores = usuarioRepo.findByRole(Role.ROLE_SUPERVISOR);

        for (Estudiante est : estudianteRepo.findAll()) {

            long ausencias = asistenciaRepo
                .findByEstudiante_IdEstudianteAndFechaBetween(
                        est.getIdEstudiante(),
                        periodoInicio,
                        periodoFin
                )
                .stream()
                .filter(a -> "AUSENTE".equals(a.getEstado()))
                .count();

            // Alerta cuando tiene más de 2 ausencias (>=3) en el período activo
            if (ausencias > 2) {

                // Si ya existe una alerta activa para este estudiante dentro del período, no spamea
                boolean yaAlertado = alertaRepo.existsByEstudianteAndTipoAndAtendidaAndFechaAlertaBetween(
                        est,
                        "INASISTENCIA",
                        false,
                        periodoInicio,
                        periodoFin
                );

                if (!yaAlertado) {
                    // Guardar alerta en BD
                    Alerta alerta = new Alerta();
                    alerta.setEstudiante(est);
                    alerta.setTipo("INASISTENCIA");
                    alerta.setFechaAlerta(new Date());
                    alerta.setAtendida(false);
                    alerta.setDescripcion(
                            "Estudiante tiene " + ausencias + " ausencias en el período: " + periodo.getNombre()
                    );
                    alertaRepo.save(alerta);

                    // Mandar correo a todos los supervisores
                    for (Usuario sup : supervisores) {
                        if (sup.getEmail() != null && !sup.getEmail().isBlank()) {
                            emailService.enviarAlertaAusentismo(
                                sup.getEmail(),
                                est.getNombre() + " " + est.getApellido(),
                                ausencias,
                                "CRITICO"
                            );
                        }
                    }

                    System.out.println("[Scheduler] Alerta enviada para: " + est.getNombre());
                }
            }
        }
    }
    @Autowired
private AsistenciaFGK.Oportunidades.repositories.GrupoRepository grupoRepo;

@Autowired
private CalendarioService calendarioService;

/**
 * Corre periódicamente.
 * - Durante el horario de clase: si no existe registro, crea PENDIENTE.
 * - Al finalizar el horario de clase: convierte PENDIENTE → AUSENTE (o crea AUSENTE si no existía).
 */
@Scheduled(fixedDelay = 300000)
@Transactional
public void actualizarPendientesYAusentesPorHorario() {

    java.time.LocalDate hoyLocal = java.time.LocalDate.now();

    // Solo gestionar asistencias dentro de un período activo
    Optional<PeriodoEscolar> periodoOpt = calendarioService.periodoActual();
    if (periodoOpt.isEmpty()) {
        return;
    }

    // No correr en días bloqueados (feriados, asuetos)
    if (calendarioService.esHoyDiaBloqueado()) {
        return;
    }

    String diaHoy = java.util.Map.of(
        java.time.DayOfWeek.MONDAY,    "LUNES",
        java.time.DayOfWeek.TUESDAY,   "MARTES",
        java.time.DayOfWeek.WEDNESDAY, "MIERCOLES",
        java.time.DayOfWeek.THURSDAY,  "JUEVES",
        java.time.DayOfWeek.FRIDAY,    "VIERNES",
        java.time.DayOfWeek.SATURDAY,  "SABADO",
        java.time.DayOfWeek.SUNDAY,    "DOMINGO"
    ).get(hoyLocal.getDayOfWeek());

    LocalTime ahora = LocalTime.now();
    java.util.Date hoy = java.sql.Date.valueOf(hoyLocal);

    for (AsistenciaFGK.Oportunidades.models.Grupo grupo : grupoRepo.findAll()) {

        // ¿Este grupo tiene clase hoy?
        String dias = grupo.getDias() != null ? grupo.getDias().toUpperCase() : "";
        if (!dias.contains(diaHoy)) continue;

        // Se requiere horario para gestionar PENDIENTE/AUSENTE por jornada
        if (grupo.getHoraInicio() == null || grupo.getHoraInicio().isBlank()
                || grupo.getHoraFin() == null || grupo.getHoraFin().isBlank()) {
            continue;
        }

        LocalTime inicio;
        LocalTime fin;
        try {
            inicio = LocalTime.parse(grupo.getHoraInicio().trim());
            fin = LocalTime.parse(grupo.getHoraFin().trim());
        } catch (Exception e) {
            continue;
        }

        // Antes de iniciar la clase, no marcar nada
        if (ahora.isBefore(inicio)) {
            continue;
        }

        boolean duranteClase = !ahora.isAfter(fin);

        // Estudiantes inscritos en este grupo
        List<AsistenciaFGK.Oportunidades.models.Estudiante> inscritos = grupo.getEstudiantes();
        if (inscritos == null || inscritos.isEmpty()) continue;

        // Registros existentes de hoy (por estudiante) para este grupo
        var registrosHoy = asistenciaRepo.findByGrupoAndFecha(grupo, hoy);
        var registroPorEstudiante = registrosHoy.stream()
                .collect(Collectors.toMap(
                        a -> a.getEstudiante().getIdEstudiante(),
                        a -> a,
                        (a1, a2) -> a1
                ));

        for (AsistenciaFGK.Oportunidades.models.Estudiante est : inscritos) {

            // Respeta modalidad/jornada cuando esté configurada en el grupo
            String modalidadGrupo = grupo.getModalidad();
            if (modalidadGrupo != null && !modalidadGrupo.isBlank()) {
                String mod = modalidadGrupo.trim().toUpperCase();
                String jornada = est.getJornada().name();
                if (!mod.equals(jornada)) {
                    continue;
                }
            }

            AsistenciaFGK.Oportunidades.models.Asistencia existente = registroPorEstudiante.get(est.getIdEstudiante());

            if (existente == null) {
                // Sin registro aún
                AsistenciaFGK.Oportunidades.models.Asistencia nuevo = new AsistenciaFGK.Oportunidades.models.Asistencia();
                nuevo.setEstudiante(est);
                nuevo.setGrupo(grupo);
                nuevo.setFecha(hoy);
                nuevo.setHoraEntrada(null);
                nuevo.setHoraSalida(null);

                if (duranteClase) {
                    nuevo.setEstado("PENDIENTE");
                } else {
                    nuevo.setEstado("AUSENTE");
                }

                asistenciaRepo.save(nuevo);
                registroPorEstudiante.put(est.getIdEstudiante(), nuevo);

            } else {
                // Con registro: si terminó la clase y estaba PENDIENTE, se marca AUSENTE
                if (!duranteClase && "PENDIENTE".equalsIgnoreCase(existente.getEstado())) {
                    existente.setEstado("AUSENTE");
                    asistenciaRepo.save(existente);
                }
            }
        }
    }
}
}
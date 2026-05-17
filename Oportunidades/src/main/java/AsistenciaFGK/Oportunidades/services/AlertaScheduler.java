package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.Alerta;
import AsistenciaFGK.Oportunidades.models.Estudiante;
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

import java.util.Date;
import java.util.List;

@Component
public class AlertaScheduler {

    @Autowired private EstudianteRepository estudianteRepo;
    @Autowired private AsistenciaRepository asistenciaRepo;
    @Autowired private AlertaRepository alertaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private EmailService emailService;

    // Corre cada hora (3600000 ms). Cambiá el número si querés más frecuencia.
    @Scheduled(fixedDelay = 60000)
    public void verificarAusentismo() {

        System.out.println("[Scheduler] Verificando ausentismo...");

        List<Usuario> supervisores = usuarioRepo.findByRole(Role.ROLE_SUPERVISOR);

        for (Estudiante est : estudianteRepo.findAll()) {

            long ausencias = asistenciaRepo
                .findByEstudiante_IdEstudiante(est.getIdEstudiante())
                .stream()
                .filter(a -> "AUSENTE".equals(a.getEstado()))
                .count();

            // Solo actúa cuando llega a exactamente 2 faltas
            if (ausencias == 2) {

                // Si ya existe una alerta activa para este estudiante, no spamea
                boolean yaAlertado = alertaRepo.existsByEstudianteAndTipoAndAtendida(
                    est, "INASISTENCIA", false
                );

                if (!yaAlertado) {
                    // Guardar alerta en BD
                    Alerta alerta = new Alerta();
                    alerta.setEstudiante(est);
                    alerta.setTipo("INASISTENCIA");
                    alerta.setFechaAlerta(new Date());
                    alerta.setAtendida(false);
                    alerta.setDescripcion("Estudiante alcanzó 2 faltas en el periodo.");
                    alertaRepo.save(alerta);

                    // Mandar correo a todos los supervisores
                    for (Usuario sup : supervisores) {
                        if (sup.getEmail() != null && !sup.getEmail().isBlank()) {
                            emailService.enviarAlertaAusentismo(
                                sup.getEmail(),
                                est.getNombre() + " " + est.getApellido(),
                                ausencias,
                                "ALERTA"
                            );
                        }
                    }

                    System.out.println("[Scheduler] Alerta enviada para: " + est.getNombre());
                }
            }
        }
    }
}
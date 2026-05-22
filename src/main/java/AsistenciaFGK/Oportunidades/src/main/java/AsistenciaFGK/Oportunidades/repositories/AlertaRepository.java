package AsistenciaFGK.Oportunidades.repositories;

import AsistenciaFGK.Oportunidades.models.Alerta;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    // Verifica si ya existe una alerta de INASISTENCIA no atendida para ese estudiante
    boolean existsByEstudianteAndTipoAndAtendida(Estudiante estudiante, String tipo, boolean atendida);

    // Evita spam: permite alertar de nuevo en un período distinto
    boolean existsByEstudianteAndTipoAndAtendidaAndFechaAlertaBetween(
            Estudiante estudiante,
            String tipo,
            boolean atendida,
            java.util.Date inicio,
            java.util.Date fin
    );

    void deleteByEstudiante(Estudiante estudiante);
}
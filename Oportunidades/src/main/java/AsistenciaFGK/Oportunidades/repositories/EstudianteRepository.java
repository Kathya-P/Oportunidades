package AsistenciaFGK.Oportunidades.repositories;

import AsistenciaFGK.Oportunidades.models.Estudiante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {
    boolean existsByCodigoBarras(String codigoBarras);
    Optional<Estudiante> findByCodigoBarras(String codigoBarras);
}
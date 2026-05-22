package AsistenciaFGK.Oportunidades.repositories;

import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.models.Inscripcion;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    void deleteByEstudiante(Estudiante estudiante);

    void deleteByGrupo(Grupo grupo);
}
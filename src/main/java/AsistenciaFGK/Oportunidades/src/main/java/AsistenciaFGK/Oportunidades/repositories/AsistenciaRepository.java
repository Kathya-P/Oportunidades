package AsistenciaFGK.Oportunidades.repositories;

import AsistenciaFGK.Oportunidades.models.Asistencia;
import AsistenciaFGK.Oportunidades.models.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
List<Asistencia> findByGrupoAndFechaBetween(Grupo grupo, Date inicio, Date fin);
    List<Asistencia> findByGrupoAndFecha(Grupo grupo, Date fecha);

    long countByGrupoAndEstado(Grupo grupo, String estado);

    List<Asistencia> findByEstudiante_IdEstudiante(Integer idEstudiante);

    @Query("SELECT a FROM Asistencia a WHERE a.estudiante.idEstudiante = :id AND a.estado = 'AUSENTE' AND a.fecha >= :inicio AND a.fecha <= :fin")
    List<Asistencia> findAusenciasPorEstudianteYSemana(
        @Param("id") Integer id,
        @Param("inicio") LocalDate inicio,
        @Param("fin") LocalDate fin
    );
    
    List<Asistencia> findByEstudiante_IdEstudianteAndFechaBetween(Integer idEstudiante, Date fechaInicio, Date fechaFin);
    /** Contar por grupo, estado y rango de fechas (para filtrar por periodo). */
    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.grupo = :grupo AND a.estado = :estado AND a.fecha >= :inicio AND a.fecha <= :fin")
    long countByGrupoAndEstadoAndFechaBetween(
        @Param("grupo") Grupo grupo,
        @Param("estado") String estado,
        @Param("inicio") Date inicio,
        @Param("fin") Date fin
    );

    /** Todas las asistencias dentro de un rango de fechas. */
    @Query("SELECT a FROM Asistencia a WHERE a.fecha >= :inicio AND a.fecha <= :fin")
    List<Asistencia> findByFechaBetween(
        @Param("inicio") Date inicio,
        @Param("fin") Date fin
    );


    /** Eliminar todas las asistencias de un estudiante (para borrado en cascada). */
    void deleteByEstudiante_IdEstudiante(Integer idEstudiante);

    /** Eliminar todas las asistencias de un grupo (para borrado en cascada). */
    void deleteByGrupo(Grupo grupo);


}
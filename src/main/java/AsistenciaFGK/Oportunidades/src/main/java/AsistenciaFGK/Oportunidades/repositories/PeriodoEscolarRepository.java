/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.repositories;

/**
 *
 * @author kathy
 */
// ─── PeriodoEscolarRepository.java ───────────────────────────────────────────

import AsistenciaFGK.Oportunidades.models.PeriodoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodoEscolarRepository extends JpaRepository<PeriodoEscolar, Integer> {

    List<PeriodoEscolar> findAllByOrderByFechaInicioDesc();

    List<PeriodoEscolar> findByActivoTrue();

    /** Devuelve el periodo activo cuya ventana contiene la fecha dada. */
    Optional<PeriodoEscolar> findFirstByActivoTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            LocalDate fecha1, LocalDate fecha2);

        boolean existsByFechaInicio(LocalDate fechaInicio);

        boolean existsByFechaInicioAndIdPeriodoNot(LocalDate fechaInicio, Integer idPeriodo);

        @Query("""
                SELECT COUNT(p) FROM PeriodoEscolar p
                WHERE (:excluirId IS NULL OR p.idPeriodo <> :excluirId)
                    AND p.fechaInicio <= :fin AND p.fechaFin >= :inicio
        """)
        long countTraslapes(@Param("inicio") LocalDate inicio,
                                                @Param("fin") LocalDate fin,
                                                @Param("excluirId") Integer excluirId);
}

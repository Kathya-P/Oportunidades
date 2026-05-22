/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.repositories;

/**
 *
 * @author kathy
 */

import AsistenciaFGK.Oportunidades.models.DiaEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DiaEspecialRepository extends JpaRepository<DiaEspecial, Integer> {

    List<DiaEspecial> findAllByOrderByFechaInicioAsc();

    /** Días especiales cuyo rango cubre la fecha indicada (para bloquear asistencia). */
    @Query("""
        SELECT d FROM DiaEspecial d
        WHERE d.fechaInicio <= :fecha AND d.fechaFin >= :fecha
    """)
    List<DiaEspecial> findByFechaEnRango(@Param("fecha") LocalDate fecha);

    /** Días especiales de un periodo dado. */
    List<DiaEspecial> findByPeriodo_IdPeriodoOrderByFechaInicioAsc(Integer periodoId);

    /** Días especiales en un rango de fechas (para renderizar el calendario). */
    @Query("""
        SELECT d FROM DiaEspecial d
        WHERE d.fechaInicio <= :fin AND d.fechaFin >= :inicio
        ORDER BY d.fechaInicio
    """)
    List<DiaEspecial> findEnRango(@Param("inicio") LocalDate inicio,
                                  @Param("fin")    LocalDate fin);
}

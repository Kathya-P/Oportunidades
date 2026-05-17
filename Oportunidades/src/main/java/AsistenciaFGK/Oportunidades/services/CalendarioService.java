/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;

/**
 *
 * @author kathy
 */

import AsistenciaFGK.Oportunidades.models.DiaEspecial;
import AsistenciaFGK.Oportunidades.models.PeriodoEscolar;
import AsistenciaFGK.Oportunidades.repositories.DiaEspecialRepository;
import AsistenciaFGK.Oportunidades.repositories.PeriodoEscolarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarioService {

    @Autowired
    private PeriodoEscolarRepository periodoRepo;

    @Autowired
    private DiaEspecialRepository diaRepo;

    // ── Periodos ──────────────────────────────────────────────────────────

    public List<PeriodoEscolar> listarPeriodos() {
        return periodoRepo.findAllByOrderByFechaInicioDesc();
    }

    public Optional<PeriodoEscolar> buscarPeriodo(Integer id) {
        return periodoRepo.findById(id);
    }

    public PeriodoEscolar guardarPeriodo(PeriodoEscolar p) {
        return periodoRepo.save(p);
    }

    public void eliminarPeriodo(Integer id) {
        periodoRepo.deleteById(id);
    }

    public boolean existePeriodoConMismaFechaInicio(LocalDate fechaInicio, Integer excluirId) {
        if (fechaInicio == null) return false;
        if (excluirId == null) return periodoRepo.existsByFechaInicio(fechaInicio);
        return periodoRepo.existsByFechaInicioAndIdPeriodoNot(fechaInicio, excluirId);
    }

    public boolean hayTraslapePeriodos(LocalDate inicio, LocalDate fin, Integer excluirId) {
        if (inicio == null || fin == null) return false;
        return periodoRepo.countTraslapes(inicio, fin, excluirId) > 0;
    }

    /** Periodo activo que contiene hoy. */
    public Optional<PeriodoEscolar> periodoActual() {
        LocalDate hoy = LocalDate.now();
        return periodoRepo
            .findFirstByActivoTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(hoy, hoy);
    }

    // ── Días especiales ────────────────────────────────────────────────────

    public List<DiaEspecial> listarDias() {
        return diaRepo.findAllByOrderByFechaInicioAsc();
    }

    public Optional<DiaEspecial> buscarDia(Integer id) {
        return diaRepo.findById(id);
    }

    public DiaEspecial guardarDia(DiaEspecial d) {
        return diaRepo.save(d);
    }

    public void eliminarDia(Integer id) {
        diaRepo.deleteById(id);
    }

    public List<DiaEspecial> diasEnRango(LocalDate inicio, LocalDate fin) {
        return diaRepo.findEnRango(inicio, fin);
    }

    // ── Validación de asistencia ──────────────────────────────────────────

    /**
     * Devuelve true si HOY es un día bloqueado para marcar asistencia
     * (festivo, asueto o semana pedagógica configurado por el admin).
     */
    public boolean esHoyDiaBloqueado() {
        return esDiaBloqueado(LocalDate.now());
    }

    /**
     * Devuelve true si la fecha indicada está bloqueada.
     */
    public boolean esDiaBloqueado(LocalDate fecha) {
        return diaRepo.findByFechaEnRango(fecha)
                .stream()
                .anyMatch(d -> Boolean.TRUE.equals(d.getBloqueaAsistencia()));
    }

    /**
     * Devuelve el motivo del bloqueo de hoy (para mostrar al alumno).
     */
    public String motivoBloqueoHoy() {
        List<DiaEspecial> dias = diaRepo.findByFechaEnRango(LocalDate.now());
        return dias.stream()
                .filter(d -> Boolean.TRUE.equals(d.getBloqueaAsistencia()))
                .findFirst()
                .map(DiaEspecial::getNombre)
                .orElse(null);
    }
}

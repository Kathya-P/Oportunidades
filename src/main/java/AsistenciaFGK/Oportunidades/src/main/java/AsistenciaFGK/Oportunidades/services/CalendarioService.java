/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;

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

    /** Periodo activo que contiene la fecha indicada (no necesariamente hoy). */
    public Optional<PeriodoEscolar> periodoParaFecha(LocalDate fecha) {
        if (fecha == null) return Optional.empty();
        return periodoRepo
            .findFirstByActivoTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(fecha, fecha);
    }

    /**
     * Devuelve el periodo activo que cubre TODO el rango [desde, hasta].
     * Si el rango toca fechas fuera de cualquier periodo activo devuelve empty.
     * Si desde y hasta pertenecen al mismo periodo, lo devuelve.
     */
    public Optional<PeriodoEscolar> periodoQueContieneRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) return Optional.empty();
        // El periodo debe contener tanto el inicio como el fin del rango
        Optional<PeriodoEscolar> periodoDesde = periodoParaFecha(desde);
        Optional<PeriodoEscolar> periodoHasta = periodoParaFecha(hasta);
        if (periodoDesde.isEmpty() || periodoHasta.isEmpty()) return Optional.empty();
        // Ambos extremos deben pertenecer al mismo periodo
        if (periodoDesde.get().getIdPeriodo().equals(periodoHasta.get().getIdPeriodo())) {
            return periodoDesde;
        }
        return Optional.empty();
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
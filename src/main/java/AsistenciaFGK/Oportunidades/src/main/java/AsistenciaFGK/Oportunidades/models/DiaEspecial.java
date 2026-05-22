/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;

/**
 *
 * @author kathy
 */

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Día especial dentro del calendario: festivo, asueto o semana pedagógica.
 * Puede configurarse para bloquear (o permitir) marcar asistencia.
 */
@Entity
@Table(name = "dias_especiales")
public class DiaEspecial {

    public enum TipoDia {
        FESTIVO,           // feriado nacional
        ASUETO,            // día de asueto institucional
        SEMANA_PEDAGOGICA  // pausa pedagógica (puede durar varios días)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDia;

    @Column(nullable = false, length = 150)
    private String nombre;           // Ej: "Semana Santa", "Día del Maestro"

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;      // Igual a fechaInicio si es un solo día

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDia tipo;

    @Column(length = 300)
    private String descripcion;

    /** Si es true, bloquea marcar asistencia en este rango. */
    @Column
    private Boolean bloqueaAsistencia = Boolean.TRUE;

    /** Periodo al que pertenece este día especial (opcional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    private PeriodoEscolar periodo;

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Integer   getIdDia()                  { return idDia; }
    public void      setIdDia(Integer v)         { this.idDia = v; }

    public String    getNombre()                 { return nombre; }
    public void      setNombre(String v)         { this.nombre = v; }

    public LocalDate getFechaInicio()            { return fechaInicio; }
    public void      setFechaInicio(LocalDate v) { this.fechaInicio = v; }

    public LocalDate getFechaFin()               { return fechaFin; }
    public void      setFechaFin(LocalDate v)    { this.fechaFin = v; }

    public TipoDia   getTipo()                   { return tipo; }
    public void      setTipo(TipoDia v)          { this.tipo = v; }

    public String    getDescripcion()            { return descripcion; }
    public void      setDescripcion(String v)    { this.descripcion = v; }

    public Boolean getBloqueaAsistencia() {
        return bloqueaAsistencia == null ? Boolean.TRUE : bloqueaAsistencia;
    }

    public void setBloqueaAsistencia(Boolean bloqueaAsistencia) {
        this.bloqueaAsistencia = bloqueaAsistencia;
    }

    public PeriodoEscolar getPeriodo()           { return periodo; }
    public void           setPeriodo(PeriodoEscolar v) { this.periodo = v; }
}

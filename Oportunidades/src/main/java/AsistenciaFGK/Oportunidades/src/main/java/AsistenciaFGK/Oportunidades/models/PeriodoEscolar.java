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
 * Periodo escolar con duración configurable en semanas.
 * El admin crea periodos; los demás roles solo los ven.
 */
@Entity
@Table(name = "periodos_escolares")
public class PeriodoEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPeriodo;

    @Column(nullable = false, length = 120)
    private String nombre;           // Ej: "Periodo I – 2025"

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;      // Calculada a partir de fechaInicio + duracionSemanas

    /** Duración del período en semanas (configurable por el admin). */
    @Column
    private Integer duracionSemanas = 10;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Integer getIdPeriodo()               { return idPeriodo; }
    public void    setIdPeriodo(Integer v)      { this.idPeriodo = v; }

    public String  getNombre()                  { return nombre; }
    public void    setNombre(String v)          { this.nombre = v; }

    public LocalDate getFechaInicio()           { return fechaInicio; }
    public void      setFechaInicio(LocalDate v){ this.fechaInicio = v; }

    public LocalDate getFechaFin()              { return fechaFin; }
    public void      setFechaFin(LocalDate v)   { this.fechaFin = v; }

    public Integer getDuracionSemanas() {
        if (duracionSemanas == null || duracionSemanas < 1) return 10;
        return duracionSemanas;
    }

    public void setDuracionSemanas(Integer duracionSemanas) {
        this.duracionSemanas = duracionSemanas;
    }

    public String  getDescripcion()             { return descripcion; }
    public void    setDescripcion(String v)     { this.descripcion = v; }

    public boolean isActivo()                   { return activo; }
    public void    setActivo(boolean v)         { this.activo = v; }
}

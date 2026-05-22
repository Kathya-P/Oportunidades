/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReporte;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaGeneracion;

    /**
     * Tipo: ASISTENCIA, INASISTENCIAS, GRUPO, etc.
     */
    @Column(nullable = false)
    private String tipo;

    @Column(length = 5000)
    private String contenido;

    /** Usuario que generó el reporte (Docente o Supervisor) */
    @ManyToOne
    @JoinColumn(name = "generado_por_id", nullable = false)
    private Usuario generadoPor;

    /** Grupo al que aplica el reporte (opcional) */
    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    // ── Getters y Setters ──────────────────────────────────────

    public Integer getIdReporte() { return idReporte; }
    public void setIdReporte(Integer idReporte) { this.idReporte = idReporte; }

    public Date getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(Date fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Usuario getGeneradoPor() { return generadoPor; }
    public void setGeneradoPor(Usuario generadoPor) { this.generadoPor = generadoPor; }

    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
}
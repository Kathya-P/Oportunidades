/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAlerta;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaAlerta;

    /**
     * Tipo: INASISTENCIA, TARDANZA, CONDUCTA, etc.
     */
    @Column(nullable = false)
    private String tipo;

    private String descripcion;

    @Column(nullable = false)
    private boolean atendida = false;

    /** Estudiante al que refiere la alerta */
    @ManyToOne
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    /** Supervisor que atendió/generó la alerta (puede ser null si aún no se atiende) */
    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    // ── Getters y Setters ──────────────────────────────────────

    public Integer getIdAlerta() { return idAlerta; }
    public void setIdAlerta(Integer idAlerta) { this.idAlerta = idAlerta; }

    public Date getFechaAlerta() { return fechaAlerta; }
    public void setFechaAlerta(Date fechaAlerta) { this.fechaAlerta = fechaAlerta; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public Usuario getSupervisor() { return supervisor; }
    public void setSupervisor(Usuario supervisor) { this.supervisor = supervisor; }
}
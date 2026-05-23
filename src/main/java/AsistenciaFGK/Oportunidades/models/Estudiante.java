/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;

import jakarta.persistence.*;
import java.util.List;

/**
 * El Alumno NO tiene login.
 * Se identifica únicamente por su código de barras en el lector físico.
 */
@Entity
@Table(name = "estudiantes")
public class Estudiante {

    public enum Jornada {
        FULL_TIME,
        SABATINO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstudiante;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String codigoBarras;   // escaneado por el lector de carnet

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Jornada jornada = Jornada.FULL_TIME;

    
    @ManyToMany
@JoinTable(
    name = "grupo_estudiantes",
    joinColumns = @JoinColumn(name = "estudiante_id"),
    inverseJoinColumns = @JoinColumn(name = "grupo_id")
)
private List<Grupo> grupos;

public List<Grupo> getGrupos() { return grupos; }
public void setGrupos(List<Grupo> grupos) { this.grupos = grupos; }

    // ── Getters y Setters ──────────────────────────────────────

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public Jornada getJornada() {
        return (jornada == null) ? Jornada.FULL_TIME : jornada;
    }

    public void setJornada(Jornada jornada) {
        this.jornada = jornada;
    }
}
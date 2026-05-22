/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "grupos")
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idGrupo;

    @Column(nullable = false)
    private String nombre;

    private String horario;
    private String dias;         // ej: "LUNES,MARTES,JUEVES"
    private String modalidad; // "FULL_TIME" o "SABATINO"
private String horaInicio;   // ej: "08:00"
private String horaFin;      // ej: "12:00"
    private Integer capacidad;

    @ManyToOne
    @JoinColumn(name = "programa_id")
    private Programa programa;

    @ManyToMany(mappedBy = "grupos")
    private List<Estudiante> estudiantes;

    // Getters y Setters
    public Integer getIdGrupo() { return idGrupo; }
    public void setIdGrupo(Integer idGrupo) { this.idGrupo = idGrupo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public Programa getPrograma() { return programa; }
    public void setPrograma(Programa programa) { this.programa = programa; }
    public List<Estudiante> getEstudiantes() { return estudiantes; }
    public void setEstudiantes(List<Estudiante> estudiantes) { this.estudiantes = estudiantes; }
    public String getDias() { return dias; }
public void setDias(String dias) { this.dias = dias; }

public String getHoraInicio() { return horaInicio; }
public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

public String getHoraFin() { return horaFin; }
public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
public String getModalidad() { return modalidad; }
public void setModalidad(String modalidad) { this.modalidad = modalidad; }
}
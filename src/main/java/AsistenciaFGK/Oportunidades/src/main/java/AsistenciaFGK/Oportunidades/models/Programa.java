/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "programas")
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrograma;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    private String sede;

    @OneToMany(mappedBy = "programa", cascade = CascadeType.ALL)
    private List<Grupo> grupos;

    // ── Getters y Setters ──────────────────────────────────────

    public Integer getIdPrograma() { return idPrograma; }
    public void setIdPrograma(Integer idPrograma) { this.idPrograma = idPrograma; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }

    public List<Grupo> getGrupos() { return grupos; }
    public void setGrupos(List<Grupo> grupos) { this.grupos = grupos; }
}
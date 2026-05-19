/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import AsistenciaFGK.Oportunidades.repositories.InscripcionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeccionService {

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    public List<Grupo> listarTodos() {
        return grupoRepository.findAll();
    }

    public Optional<Grupo> buscarPorId(Integer id) {
        return grupoRepository.findById(id);
    }

    public void guardar(Grupo grupo) {
        grupoRepository.save(grupo);
    }

    @Transactional
    public void eliminar(Integer id) {
        Grupo grupo = grupoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        // Borrar en orden: tablas hijas primero
        asistenciaRepository.deleteByGrupo(grupo);
        inscripcionRepository.deleteByGrupo(grupo);
        grupoRepository.deleteById(id);
    }
}
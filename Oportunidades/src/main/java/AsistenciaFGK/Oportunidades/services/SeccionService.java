/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;

/**
 *
 * @author kathy
 */
import AsistenciaFGK.Oportunidades.models.Grupo;
import AsistenciaFGK.Oportunidades.repositories.GrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeccionService {

    @Autowired
    private GrupoRepository grupoRepository;

    public List<Grupo> listarTodos() {
        return grupoRepository.findAll();
    }

    public Optional<Grupo> buscarPorId(Integer id) {
        return grupoRepository.findById(id);
    }

    public void guardar(Grupo grupo) {
        grupoRepository.save(grupo);
    }

    public void eliminar(Integer id) {
        grupoRepository.deleteById(id);
    }
}
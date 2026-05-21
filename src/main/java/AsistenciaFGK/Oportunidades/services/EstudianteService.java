/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;

import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.models.Estudiante;
import AsistenciaFGK.Oportunidades.repositories.AlertaRepository;
import AsistenciaFGK.Oportunidades.repositories.AsistenciaRepository;
import AsistenciaFGK.Oportunidades.repositories.EstudianteRepository;
import AsistenciaFGK.Oportunidades.repositories.InscripcionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstudianteService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    public List<Estudiante> listarTodos() {
        return estudianteRepository.findAll();
    }

    public Optional<Estudiante> buscarPorId(Integer id) {
        return estudianteRepository.findById(id);
    }

    public Optional<Estudiante> buscarPorCodigoBarras(String codigoBarras) {
        return estudianteRepository.findByCodigoBarras(codigoBarras);
    }

    public void guardar(Estudiante estudiante) {
        estudianteRepository.save(estudiante);
    }

    @Transactional
    public void eliminar(Integer id) {
        Estudiante est = estudianteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        // Borrar en orden: primero tablas hijas, luego el estudiante
        alertaRepository.deleteByEstudiante(est);
        asistenciaRepository.deleteByEstudiante_IdEstudiante(id);
        inscripcionRepository.deleteByEstudiante(est);
        estudianteRepository.deleteById(id);
    }

    public boolean existeCodigoBarras(String codigoBarras) {
        return estudianteRepository.existsByCodigoBarras(codigoBarras);
    }
}
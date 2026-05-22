/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.services;


import AsistenciaFGK.Oportunidades.models.Role;
import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarPorRol(Role role) {
        return usuarioRepository.findByRole(role);
    }

    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    public void guardar(Usuario usuario) {
        if (usuario.getIdUsuario() == null) {
            // Usuario nuevo — encriptar siempre
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        } else {
            String passwordActual = usuario.getPassword();
            if (passwordActual == null || passwordActual.isEmpty()) {
                // Sin cambio — conservar la actual
                Usuario existente = usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow();
                usuario.setPassword(existente.getPassword());
            } else if (!passwordActual.startsWith("$2a$")) {
                // Solo encriptar si NO viene ya encriptada (BCrypt empieza con $2a$)
                usuario.setPassword(passwordEncoder.encode(passwordActual));
            }
            // Si empieza con $2a$ ya viene encriptada del reset — guardar tal cual
        }
        usuarioRepository.save(usuario);
    }

    public void eliminar(Integer id) {
        usuarioRepository.deleteById(id);
    }

    public boolean existeUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }
}

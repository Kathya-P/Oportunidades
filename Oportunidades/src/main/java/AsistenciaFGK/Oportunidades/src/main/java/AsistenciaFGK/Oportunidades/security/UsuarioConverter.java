/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.security;

import AsistenciaFGK.Oportunidades.models.Usuario;
import AsistenciaFGK.Oportunidades.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UsuarioConverter implements Converter<String, Usuario> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Usuario convert(String id) {
        if (id == null || id.isEmpty()) return null;
        return usuarioRepository.findById(Integer.parseInt(id)).orElse(null);
    }
}

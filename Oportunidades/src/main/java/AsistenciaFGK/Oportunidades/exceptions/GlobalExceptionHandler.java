package AsistenciaFGK.Oportunidades.exceptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Manejo general
    @ExceptionHandler(Exception.class)
    public String manejarExcepcionGeneral(Exception ex, Model model) {

        model.addAttribute("titulo", "Ocurrió un error");
        model.addAttribute("mensaje", ex.getMessage());

        return "error";
    }

    // RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public String manejarRuntime(RuntimeException ex, Model model) {

        model.addAttribute("titulo", "Error del sistema");
        model.addAttribute("mensaje", ex.getMessage());

        return "error";
    }
}
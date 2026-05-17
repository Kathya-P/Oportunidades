package AsistenciaFGK.Oportunidades.exceptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }

    // Manejo general
    @ExceptionHandler(Exception.class)
    public String manejarExcepcionGeneral(Exception ex, Model model) {

        model.addAttribute("titulo", "Ocurrió un error");
        model.addAttribute("mensaje", safeMessage(ex));

        return "error";
    }

    // RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public String manejarRuntime(RuntimeException ex, Model model) {

        model.addAttribute("titulo", "Error del sistema");
        model.addAttribute("mensaje", safeMessage(ex));

        return "error";
    }
}
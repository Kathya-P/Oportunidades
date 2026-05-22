/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.controllers;

import AsistenciaFGK.Oportunidades.services.CalendarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class DashboardController {

    @Autowired
    private CalendarioService calendarioService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        YearMonth ym = YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        model.addAttribute("calHoy", LocalDate.now());
        model.addAttribute("calAnio", ym.getYear());
        model.addAttribute("calMes", ym.getMonthValue());
        model.addAttribute("calDiasEnMes", ym.lengthOfMonth());
        model.addAttribute("calPrimerDia", inicio.getDayOfWeek().getValue() % 7);
        model.addAttribute("calPeriodoActual", calendarioService.periodoActual().orElse(null));
        model.addAttribute("calDiasEsp", calendarioService.diasEnRango(inicio, fin));
        model.addAttribute("calEsBloqueado", calendarioService.esHoyDiaBloqueado());
        model.addAttribute("calMotivoBloqueo", calendarioService.motivoBloqueoHoy());

        return "dashboard";
    }

}
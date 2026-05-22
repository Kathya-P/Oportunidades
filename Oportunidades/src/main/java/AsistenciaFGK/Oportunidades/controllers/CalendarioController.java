/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.controllers;

/**
 *
 * @author kathy
 */

import AsistenciaFGK.Oportunidades.models.DiaEspecial;
import AsistenciaFGK.Oportunidades.models.PeriodoEscolar;
import AsistenciaFGK.Oportunidades.services.CalendarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    @Autowired
    private CalendarioService calendarioService;

    // ── Vista pública del calendario (todos los roles) ─────────────────────

    @GetMapping
    public String verCalendario(
            @RequestParam(value = "anio",  required = false) Integer anio,
            @RequestParam(value = "mes",   required = false) Integer mes,
            Model model) {

        YearMonth ym = (anio != null && mes != null)
                ? YearMonth.of(anio, mes)
                : YearMonth.now();

        LocalDate inicio = ym.atDay(1);
        LocalDate fin    = ym.atEndOfMonth();

        List<DiaEspecial>   dias     = calendarioService.diasEnRango(inicio, fin);
        List<PeriodoEscolar> periodos = calendarioService.listarPeriodos();

        model.addAttribute("anio",          ym.getYear());
        model.addAttribute("mes",           ym.getMonthValue());
        model.addAttribute("diasEnMes",     ym.lengthOfMonth());
        model.addAttribute("primerDiaSemana", inicio.getDayOfWeek().getValue() % 7); // 0=Dom
        model.addAttribute("diasEspeciales", dias);
        model.addAttribute("periodos",       periodos);
        model.addAttribute("periodoActual",  calendarioService.periodoActual().orElse(null));
        model.addAttribute("hoy",            LocalDate.now());

        // mes anterior / siguiente para navegación
        YearMonth anterior = ym.minusMonths(1);
        YearMonth siguiente = ym.plusMonths(1);
        model.addAttribute("anioAnterior",  anterior.getYear());
        model.addAttribute("mesAnterior",   anterior.getMonthValue());
        model.addAttribute("anioSiguiente", siguiente.getYear());
        model.addAttribute("mesSiguiente",  siguiente.getMonthValue());

        return "calendario/calendario";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ADMINISTRACIÓN — solo ADMIN
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCalendario(Model model) {
        model.addAttribute("periodos", calendarioService.listarPeriodos());
        model.addAttribute("dias",     calendarioService.listarDias());
        return "admin-calendario";
    }

    // ── Periodos ──────────────────────────────────────────────────────────

    @GetMapping("/admin/periodos/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String nuevoPeriodo(Model model) {
        model.addAttribute("periodo", new PeriodoEscolar());
        model.addAttribute("titulo",  "Nuevo período escolar");
        return "periodo-form";
    }

    @GetMapping("/admin/periodos/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarPeriodo(@PathVariable Integer id, Model model) {
        PeriodoEscolar p = calendarioService.buscarPeriodo(id)
                .orElseThrow(() -> new RuntimeException("Período no encontrado"));
        model.addAttribute("periodo", p);
        model.addAttribute("titulo",  "Editar período escolar");
        return "periodo-form";
    }

    @PostMapping("/admin/periodos/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarPeriodo(@ModelAttribute PeriodoEscolar periodo,
                                 Model model,
                                 RedirectAttributes redirectAttrs) {
        if (periodo.getFechaInicio() == null) {
            model.addAttribute("periodo", periodo);
            model.addAttribute("titulo", (periodo.getIdPeriodo() == null) ? "Nuevo período escolar" : "Editar período escolar");
            model.addAttribute("error", "La fecha de inicio es obligatoria.");
            return "periodo-form";
        }

        int semanas = periodo.getDuracionSemanas();
        periodo.setFechaFin(periodo.getFechaInicio().plusWeeks(semanas).minusDays(1));

        if (calendarioService.existePeriodoConMismaFechaInicio(periodo.getFechaInicio(), periodo.getIdPeriodo())) {
            model.addAttribute("periodo", periodo);
            model.addAttribute("titulo", (periodo.getIdPeriodo() == null) ? "Nuevo período escolar" : "Editar período escolar");
            model.addAttribute("error", "No puedes crear dos períodos que empiecen en la misma fecha.");
            return "periodo-form";
        }

        if (calendarioService.hayTraslapePeriodos(periodo.getFechaInicio(), periodo.getFechaFin(), periodo.getIdPeriodo())) {
            model.addAttribute("periodo", periodo);
            model.addAttribute("titulo", (periodo.getIdPeriodo() == null) ? "Nuevo período escolar" : "Editar período escolar");
            model.addAttribute("error", "Las fechas de este período se traslapan con otro período existente.");
            return "periodo-form";
        }

        calendarioService.guardarPeriodo(periodo);
        redirectAttrs.addFlashAttribute("exito", "Período guardado correctamente.");
        return "redirect:/calendario/admin";
    }

    @GetMapping("/admin/periodos/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarPeriodo(@PathVariable Integer id,
                                   RedirectAttributes redirectAttrs) {
        calendarioService.eliminarPeriodo(id);
        redirectAttrs.addFlashAttribute("exito", "Período eliminado.");
        return "redirect:/calendario/admin";
    }

    // ── Días especiales ────────────────────────────────────────────────────

    @GetMapping("/admin/dias/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String nuevoDia(Model model) {
        model.addAttribute("dia",      new DiaEspecial());
        model.addAttribute("periodos", calendarioService.listarPeriodos());
        model.addAttribute("tipos",    DiaEspecial.TipoDia.values());
        model.addAttribute("titulo",   "Nuevo día especial");
        return "dia-form";
    }

    @GetMapping("/admin/dias/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarDia(@PathVariable Integer id, Model model) {
        DiaEspecial d = calendarioService.buscarDia(id)
                .orElseThrow(() -> new RuntimeException("Día especial no encontrado"));
        model.addAttribute("dia",      d);
        model.addAttribute("periodos", calendarioService.listarPeriodos());
        model.addAttribute("tipos",    DiaEspecial.TipoDia.values());
        model.addAttribute("titulo",   "Editar día especial");
        return "dia-form";
    }

    @PostMapping("/admin/dias/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarDia(
            @RequestParam("nombre")       String nombre,
            @RequestParam("fechaInicio")  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam("tipo")         DiaEspecial.TipoDia tipo,
            @RequestParam(value = "bloqueaAsistencia", required = false) String bloqueaAsistencia,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "periodoId",   required = false) Integer periodoId,
            @RequestParam(value = "idDia",       required = false) Integer idDia,
            Model model,
            RedirectAttributes redirectAttrs) {

        DiaEspecial dia = (idDia != null)
                ? calendarioService.buscarDia(idDia).orElse(new DiaEspecial())
                : new DiaEspecial();

        dia.setNombre(nombre);
        dia.setTipo(tipo);
        dia.setBloqueaAsistencia(bloqueaAsistencia != null);
        dia.setDescripcion(descripcion);

        PeriodoEscolar periodoSeleccionado = null;
        if (periodoId != null) {
            periodoSeleccionado = calendarioService.buscarPeriodo(periodoId).orElse(null);
        }

        if (tipo == DiaEspecial.TipoDia.SEMANA_PEDAGOGICA) {
            if (periodoSeleccionado == null || periodoSeleccionado.getFechaFin() == null) {
                dia.setFechaInicio(fechaInicio);
                dia.setFechaFin((fechaFin == null || fechaFin.isBefore(fechaInicio)) ? fechaInicio : fechaFin);
                dia.setPeriodo(periodoSeleccionado);
                model.addAttribute("dia", dia);
                model.addAttribute("periodos", calendarioService.listarPeriodos());
                model.addAttribute("tipos", DiaEspecial.TipoDia.values());
                model.addAttribute("titulo", (idDia == null) ? "Nuevo día especial" : "Editar día especial");
                model.addAttribute("error", "Para Semana Pedagógica debes seleccionar un período escolar.");
                return "dia-form";
            }
            LocalDate inicioSP = periodoSeleccionado.getFechaFin().plusDays(1);
            LocalDate finSP = inicioSP.plusDays(6);
            dia.setFechaInicio(inicioSP);
            dia.setFechaFin(finSP);
            dia.setPeriodo(periodoSeleccionado);
        } else {
            dia.setFechaInicio(fechaInicio);
            if (fechaFin == null || fechaFin.isBefore(fechaInicio)) {
                dia.setFechaFin(fechaInicio);
            } else {
                dia.setFechaFin(fechaFin);
            }
            dia.setPeriodo(periodoSeleccionado);
        }

        calendarioService.guardarDia(dia);
        redirectAttrs.addFlashAttribute("exito", "Día especial guardado correctamente.");
        return "redirect:/calendario/admin";
    }

    @GetMapping("/admin/dias/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarDia(@PathVariable Integer id,
                               RedirectAttributes redirectAttrs) {
        calendarioService.eliminarDia(id);
        redirectAttrs.addFlashAttribute("exito", "Día especial eliminado.");
        return "redirect:/calendario/admin";
    }
}
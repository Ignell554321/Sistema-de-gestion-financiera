package com.mguevara.librocontable.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mguevara.librocontable.Services.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;

    @GetMapping
    public ResponseEntity<?> generarReporte(
            @RequestParam(required = false) Long mesId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) BigDecimal montoMin,
            @RequestParam(required = false) BigDecimal montoMax) {

        if (mesId != null) {
            return ResponseEntity.ok(service.generarReportePorMes(
                    mesId,
                    categoria,
                    fechaInicio,
                    fechaFin,
                    montoMin,
                    montoMax
            ));
        }

        return ResponseEntity.ok(service.generarReporteGlobal(
                categoria,
                fechaInicio,
                fechaFin,
                montoMin,
                montoMax
        ));
    }
}

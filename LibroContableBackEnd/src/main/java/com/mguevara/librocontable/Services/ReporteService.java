package com.mguevara.librocontable.Services;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.mguevara.librocontable.Dto.Response.ReporteGlobalResponse;
import com.mguevara.librocontable.Dto.Response.ReporteMesResponse;

public interface ReporteService {

    ReporteMesResponse generarReportePorMes(
            Long mesId,
            String categoria,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax
    );

    ReporteGlobalResponse generarReporteGlobal(
            String categoria,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax
    );
}

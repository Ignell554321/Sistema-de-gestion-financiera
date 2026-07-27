package com.mguevara.librocontable.Dto.Response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteGlobalResponse {

    private BigDecimal saldoInicialGlobal;

    private BigDecimal totalGastadoGlobal;

    private BigDecimal saldoFinalGlobal;

    private Integer cantidadMeses;

    private Integer cantidadMovimientos;

    private BigDecimal totalMovimientosFiltrados;

    private List<ReporteMesResponse> meses;
}

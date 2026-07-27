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
public class ReporteMesResponse {

    private Long mesId;

    private String periodo;

    private String estado;

    private BigDecimal saldoInicial;

    private BigDecimal totalGastado;

    private BigDecimal saldoFinal;

    private Integer cantidadMovimientos;

    private BigDecimal totalMovimientosFiltrados;

    private List<MovimientoDetalleResponse> movimientos;
}

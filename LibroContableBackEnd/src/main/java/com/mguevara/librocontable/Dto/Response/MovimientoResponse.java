package com.mguevara.librocontable.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoResponse {

    private Long id;

    private Long mesId;

    private LocalDate fechaGasto;

    private String descripcion;

    private BigDecimal monto;

    private String categoria;

    private BigDecimal totalGastadoMes;

    private BigDecimal saldoDisponible;

    private String mensaje;

}

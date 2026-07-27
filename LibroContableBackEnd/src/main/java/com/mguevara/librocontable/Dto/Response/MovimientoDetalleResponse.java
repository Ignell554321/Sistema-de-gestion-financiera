package com.mguevara.librocontable.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoDetalleResponse {

    private Long id;

    private Long mesId;

    private LocalDate fechaGasto;

    private String descripcion;

    private BigDecimal monto;

    private String categoria;
}

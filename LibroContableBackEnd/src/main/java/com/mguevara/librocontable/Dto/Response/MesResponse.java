package com.mguevara.librocontable.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesResponse {

    private Long id;

    private String periodo;

    private BigDecimal saldoInicial;

    private BigDecimal totalGastado;

    private BigDecimal saldoFinal;

    private String estado;

    private Integer cantidadMovimientos;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaCierre;

}
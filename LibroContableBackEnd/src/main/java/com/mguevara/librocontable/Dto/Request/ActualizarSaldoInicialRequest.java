package com.mguevara.librocontable.Dto.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ActualizarSaldoInicialRequest {

    @NotNull(message = "El saldo inicial es obligatorio")
    @Positive(message = "El saldo inicial debe ser mayor que cero")
    private BigDecimal saldoInicial;
}

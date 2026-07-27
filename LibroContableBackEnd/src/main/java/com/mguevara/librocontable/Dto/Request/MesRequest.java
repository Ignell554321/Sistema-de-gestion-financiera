package com.mguevara.librocontable.Dto.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MesRequest {
    
    @NotBlank(message = "El período es obligatorio")
    @Pattern(
        regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
        message = "El período debe tener el formato yyyy-MM"
    )
    private String periodo;

    @NotNull(message = "El saldo inicial es obligatorio")
    @Positive(message = "El saldo inicial debe ser mayor que cero")
    private BigDecimal saldoInicial;
}

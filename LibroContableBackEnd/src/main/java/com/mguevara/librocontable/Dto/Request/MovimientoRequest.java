package com.mguevara.librocontable.Dto.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoRequest{

    @NotNull(message = "El mes es obligatorio")
    private Long mesId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(min = 3, max = 120, message = "La descripcion debe tener entre 3 y 120 caracteres")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor que cero")
    private BigDecimal monto;

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 60, message = "La categoria no debe superar los 60 caracteres")
    private String categoria;

}

package com.mguevara.librocontable.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ErrorDetail {

    private String campo;

    private String mensaje;

}
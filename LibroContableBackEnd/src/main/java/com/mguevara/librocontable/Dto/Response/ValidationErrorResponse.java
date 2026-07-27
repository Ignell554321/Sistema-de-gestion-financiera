package com.mguevara.librocontable.Dto.Response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ValidationErrorResponse {

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private String path;

    private List<ErrorDetail> errores;

}
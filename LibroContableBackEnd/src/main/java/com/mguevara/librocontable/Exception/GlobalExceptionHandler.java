package com.mguevara.librocontable.Exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.mguevara.librocontable.Dto.Response.ErrorDetail;
import com.mguevara.librocontable.Dto.Response.ErrorResponse;
import com.mguevara.librocontable.Dto.Response.ValidationErrorResponse;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Validaciones del @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request){

        List<ErrorDetail> errores =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::mapFieldError)
                        .toList();

        ValidationErrorResponse response =
                ValidationErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .path(request.getRequestURI())
                        .errores(errores)
                        .build();

        return ResponseEntity.badRequest().body(response);

    }

    /**
     * Parámetros obligatorios
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getParameterName()+" es obligatorio.",
                request
        );

    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.BAD_REQUEST,
                "El parametro " + ex.getName() + " tiene un formato invalido.",
                request
        );

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no tiene un formato valido.",
                request
        );

    }

    /**
     * Validaciones de PathVariable o RequestParam
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

    }

    /**
     * Error de integridad
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.CONFLICT,
                "Violación de integridad de datos.",
                request
        );

    }

    /**
     * Error BD
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(
            DataAccessException ex,
            HttpServletRequest request){

        log.error("Error de base de datos en {}", request.getRequestURI(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error al acceder a la base de datos.",
                request
        );

    }

    /**
     * NullPointer
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(
            NullPointerException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se produjo un error inesperado.",
                request
        );

    }

    /**
     * IllegalArgument
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegal(
            IllegalArgumentException ex,
            HttpServletRequest request){

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

    }

    /**
     * Excepciones relacionadas al módulo de Mes Financiero
     */
    @ExceptionHandler(MesException.class)
    public ResponseEntity<ErrorResponse> handleMesException(
            MesException ex,
            HttpServletRequest request){

        return build(
                ex.getStatus(),
                ex.getMessage(),
                request
        );

    }

    /**
     * Cualquier excepción
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request){

        log.error("Error inesperado en {}", request.getRequestURI(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado.",
                request
        );

    }

    private ErrorDetail mapFieldError(FieldError error){

        return ErrorDetail.builder()
                .campo(error.getField())
                .mensaje(error.getDefaultMessage())
                .build();

    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request){

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(status).body(response);

    }

}

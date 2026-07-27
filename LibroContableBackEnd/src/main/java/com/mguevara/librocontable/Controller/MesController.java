package com.mguevara.librocontable.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.mguevara.librocontable.Dto.Request.ActualizarSaldoInicialRequest;
import com.mguevara.librocontable.Dto.Request.MesRequest;
import com.mguevara.librocontable.Dto.Response.ErrorResponse;
import com.mguevara.librocontable.Dto.Response.MesResponse;
import com.mguevara.librocontable.Services.MesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class MesController {

    private final MesService service;

    @PostMapping
    public ResponseEntity<MesResponse> crear(@RequestBody @Valid MesRequest request){

        return ResponseEntity.ok(service.crear(request));
        
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<MesResponse> cerrar(@PathVariable Long id) {

        return ResponseEntity.ok(service.cerrar(id));
    }

    @Operation(
            summary = "Reabre un periodo financiero cerrado",
            description = "Permite reabrir un periodo CERRADO solo cuando no existe otro periodo en estado ABIERTO."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Periodo reabierto correctamente"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Periodo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Operacion no permitida porque ya existe un periodo abierto o el periodo ya esta abierto",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{id}/reabrir")
    public ResponseEntity<MesResponse> reabrir(@PathVariable Long id) {

        return ResponseEntity.ok(service.reabrir(id));
    }

    @Operation(
            summary = "Actualiza el saldo inicial de un periodo financiero",
            description = "Permite actualizar el saldo inicial solo si el periodo se encuentra ABIERTO. El saldo final se recalcula manteniendo intacto el total gastado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo inicial actualizado correctamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud invalida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Periodo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Operacion no permitida para un periodo cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{id}/saldo-inicial")
    public ResponseEntity<MesResponse> actualizarSaldoInicial(
            @PathVariable Long id,
            @RequestBody @Valid ActualizarSaldoInicialRequest request) {

        return ResponseEntity.ok(service.actualizarSaldoInicial(id, request));
    }

    
    @GetMapping
    public List<MesResponse> listarMeses() {
        return service.listarMeses();
    }

    @Operation(
            summary = "Elimina completamente un periodo financiero",
            description = "Elimina el mes financiero y todos sus movimientos asociados, sin importar si el mes esta ABIERTO o CERRADO."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Periodo eliminado correctamente"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Periodo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    
}

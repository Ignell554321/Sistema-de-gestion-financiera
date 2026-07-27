package com.mguevara.librocontable.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mguevara.librocontable.Dto.Request.MovimientoRequest;
import com.mguevara.librocontable.Dto.Response.MovimientoResponse;
import com.mguevara.librocontable.Entity.Movimiento;
import com.mguevara.librocontable.Services.MovimientoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService service;

    @PostMapping
    public ResponseEntity<MovimientoResponse> registrar(
            @RequestBody @Valid MovimientoRequest request){

        return ResponseEntity.ok(service.registrar(request));

    }

    @PutMapping("/{id}")
    public ResponseEntity<MovimientoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid MovimientoRequest request) {

        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping
    public List<Movimiento> listarPorMes(@RequestParam Long mesId) {
        return service.listarPorMes(mesId);
    }

}

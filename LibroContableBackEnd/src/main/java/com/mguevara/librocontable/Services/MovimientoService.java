package com.mguevara.librocontable.Services;

import java.util.List;

import com.mguevara.librocontable.Dto.Request.MovimientoRequest;
import com.mguevara.librocontable.Dto.Response.MovimientoResponse;
import com.mguevara.librocontable.Entity.Movimiento;

public interface MovimientoService {

    MovimientoResponse registrar(MovimientoRequest request);

    MovimientoResponse actualizar(Long id, MovimientoRequest request);

    void eliminar(Long id);

    List<Movimiento> listarPorMes(Long mesId);

}

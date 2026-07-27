package com.mguevara.librocontable.Services;

import java.util.List;

import com.mguevara.librocontable.Dto.Request.ActualizarSaldoInicialRequest;
import com.mguevara.librocontable.Dto.Request.MesRequest;
import com.mguevara.librocontable.Dto.Response.MesResponse;

public interface MesService {
    
     MesResponse crear(MesRequest request);

     MesResponse cerrar(Long id);

     MesResponse reabrir(Long id);

     MesResponse actualizarSaldoInicial(Long id, ActualizarSaldoInicialRequest request);

     List<MesResponse> listarMeses();

     void eliminar(Long id);
}

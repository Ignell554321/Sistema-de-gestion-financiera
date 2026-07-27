package com.mguevara.librocontable.Services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.mguevara.librocontable.Dto.Request.MovimientoRequest;
import com.mguevara.librocontable.Dto.Response.MovimientoResponse;
import com.mguevara.librocontable.Entity.EstadoMes;
import com.mguevara.librocontable.Entity.MesFinanciero;
import com.mguevara.librocontable.Entity.Movimiento;
import com.mguevara.librocontable.Exception.MesException;
import com.mguevara.librocontable.Repository.MesRepository;
import com.mguevara.librocontable.Repository.MovimientoRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoRepository repository;
    private final MesRepository mesRepository;

    @Override
    @Transactional
    public MovimientoResponse registrar(MovimientoRequest request) {

        MesFinanciero mes = obtenerMesAbierto(request.getMesId());

        Movimiento movimiento = Movimiento.builder()
                .mes(mes)
                .fechaGasto(request.getFecha())
                .descripcion(request.getDescripcion())
                .monto(request.getMonto())
                .categoria(request.getCategoria())
                .build();

        Movimiento movimientoGuardado = repository.save(movimiento);
        MesFinanciero mesActualizado = recalcularMes(mes.getId());

        return toResponse(movimientoGuardado, mesActualizado, "Movimiento registrado correctamente.");
    }

    @Override
    @Transactional
    public MovimientoResponse actualizar(Long id, MovimientoRequest request) {

        Movimiento movimiento = obtenerMovimiento(id);
        MesFinanciero mesActual = movimiento.getMes();
        validarMesAbierto(mesActual);

        MesFinanciero mesDestino = mesActual;
        if (!mesActual.getId().equals(request.getMesId())) {
            mesDestino = obtenerMesAbierto(request.getMesId());
        }

        movimiento.setMes(mesDestino);
        movimiento.setFechaGasto(request.getFecha());
        movimiento.setDescripcion(request.getDescripcion());
        movimiento.setMonto(request.getMonto());
        movimiento.setCategoria(request.getCategoria());

        Movimiento movimientoActualizado = repository.save(movimiento);

        if (!mesActual.getId().equals(mesDestino.getId())) {
            recalcularMes(mesActual.getId());
        }

        MesFinanciero mesRecalculado = recalcularMes(mesDestino.getId());

        return toResponse(movimientoActualizado, mesRecalculado, "Movimiento actualizado correctamente.");
    }

    @Override
    @Transactional
    public void eliminar(Long id) {

        Movimiento movimiento = obtenerMovimiento(id);
        MesFinanciero mes = movimiento.getMes();
        validarMesAbierto(mes);

        repository.delete(movimiento);
        repository.flush();
        recalcularMes(mes.getId());
    }

    @Override
    public List<Movimiento> listarPorMes(Long mesId) {

        if (!mesRepository.existsById(mesId)) {
            throw new MesException("Mes no encontrado con id: " + mesId, HttpStatus.NOT_FOUND);
        }

        return repository.findByMesIdOrderByFechaGastoDescIdDesc(mesId);
    }

    private Movimiento obtenerMovimiento(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new MesException("Movimiento no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }

    private MesFinanciero obtenerMesAbierto(Long mesId) {
        MesFinanciero mes = mesRepository.findById(mesId)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + mesId, HttpStatus.NOT_FOUND));

        validarMesAbierto(mes);
        return mes;
    }

    private void validarMesAbierto(MesFinanciero mes) {
        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new MesException("No se permiten operaciones sobre un mes cerrado.", HttpStatus.CONFLICT);
        }
    }

    private MesFinanciero recalcularMes(Long mesId) {
        mesRepository.actualizarTotales(mesId);

        return mesRepository.findById(mesId)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + mesId, HttpStatus.NOT_FOUND));
    }

    private MovimientoResponse toResponse(Movimiento movimiento, MesFinanciero mes, String mensaje) {
        return MovimientoResponse.builder()
                .id(movimiento.getId())
                .mesId(mes.getId())
                .fechaGasto(movimiento.getFechaGasto())
                .descripcion(movimiento.getDescripcion())
                .monto(movimiento.getMonto())
                .categoria(movimiento.getCategoria())
                .totalGastadoMes(mes.getTotalGastado())
                .saldoDisponible(mes.getSaldoFinal())
                .mensaje(mensaje)
                .build();
    }
}

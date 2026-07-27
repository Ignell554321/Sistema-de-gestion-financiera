package com.mguevara.librocontable.Services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.mguevara.librocontable.Dto.Request.ActualizarSaldoInicialRequest;
import com.mguevara.librocontable.Dto.Request.MesRequest;
import com.mguevara.librocontable.Dto.Response.MesResponse;
import com.mguevara.librocontable.Entity.EstadoMes;
import com.mguevara.librocontable.Entity.MesFinanciero;
import com.mguevara.librocontable.Exception.MesException;
import com.mguevara.librocontable.Repository.MesRepository;
import com.mguevara.librocontable.Repository.MovimientoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MesServiceImpl implements MesService {

    private final MesRepository repository;
    private final MovimientoRepository movimientoRepository;

    @Override
    public MesResponse crear(MesRequest request) {

        repository.findByPeriodo(request.getPeriodo())
                .ifPresent(m -> {
                    throw new MesException("El periodo ya existe.", HttpStatus.CONFLICT);
                });
        
        if (repository.existsByEstado(EstadoMes.ABIERTO)) {
            throw new MesException(
                    "Debe cerrar el mes actual antes de crear uno nuevo.",
                    HttpStatus.CONFLICT
            );
        }

        BigDecimal saldoInicial = obtenerSaldoInicial(request);

        MesFinanciero mes = MesFinanciero.builder()
                .periodo(request.getPeriodo())
                .saldoInicial(saldoInicial)
                .saldoFinal(saldoInicial)
                .totalGastado(BigDecimal.ZERO)
                .estado(EstadoMes.ABIERTO)
                .build();

        return toResponse(repository.save(mes));
    }

    @Override
    public MesResponse cerrar(Long id) {

        MesFinanciero mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new MesException("El mes ya se encuentra cerrado.", HttpStatus.CONFLICT);
        }

        repository.actualizarTotales(id);
        mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        mes.setEstado(EstadoMes.CERRADO);

        return toResponse(repository.save(mes));
    }

    @Override
    public MesResponse reabrir(Long id) {

        MesFinanciero mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        if (mes.getEstado() == EstadoMes.ABIERTO) {
            throw new MesException("El mes ya se encuentra abierto.", HttpStatus.CONFLICT);
        }

        if (repository.existsByEstadoAndIdNot(EstadoMes.ABIERTO, id)) {
            throw new MesException("No se puede reabrir el mes porque ya existe un mes abierto.", HttpStatus.CONFLICT);
        }

        repository.actualizarTotales(id);
        mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        mes.setEstado(EstadoMes.ABIERTO);

        log.info("Periodo financiero reabierto id={}, periodo={}", mes.getId(), mes.getPeriodo());

        return toResponse(repository.save(mes));
    }

    @Override
    public MesResponse actualizarSaldoInicial(Long id, ActualizarSaldoInicialRequest request) {

        MesFinanciero mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        if (mes.getEstado() == EstadoMes.CERRADO) {
            throw new MesException("No se puede actualizar el saldo inicial de un mes cerrado.", HttpStatus.CONFLICT);
        }

        BigDecimal saldoAnterior = mes.getSaldoInicial();
        BigDecimal nuevoSaldoInicial = request.getSaldoInicial();

        mes.setSaldoInicial(nuevoSaldoInicial);
        mes.setSaldoFinal(calcularSaldoFinal(nuevoSaldoInicial, mes.getTotalGastado()));

        log.info("Saldo inicial actualizado para mes id={}, periodo={}, saldoAnterior={}, saldoNuevo={}, saldoFinal={}",
                mes.getId(), mes.getPeriodo(), saldoAnterior, mes.getSaldoInicial(), mes.getSaldoFinal());

        return toResponse(repository.save(mes));
    }

    @Override
    public List<MesResponse> listarMeses() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void eliminar(Long id) {

        MesFinanciero mes = repository.findById(id)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + id, HttpStatus.NOT_FOUND));

        log.info("Iniciando eliminacion completa del periodo financiero id={}, periodo={}, estado={}",
                mes.getId(), mes.getPeriodo(), mes.getEstado());

        int movimientosEliminados = movimientoRepository.deleteByMesId(id);
        repository.delete(mes);

        log.info("Periodo financiero eliminado id={}, periodo={}, movimientosEliminados={}",
                id, mes.getPeriodo(), movimientosEliminados);
    }

    private BigDecimal obtenerSaldoInicial(MesRequest request) {
        Optional<MesFinanciero> ultimoMes =
                repository.findTopByEstadoAndPeriodoLessThanOrderByPeriodoDesc(
                        EstadoMes.CERRADO,
                        request.getPeriodo()
                );

        BigDecimal saldoDisponibleMesAnterior = ultimoMes
                .map(MesFinanciero::getSaldoFinal)
                .map(this::valorSeguro)
                .orElse(BigDecimal.ZERO);

        return request.getSaldoInicial().add(saldoDisponibleMesAnterior);
    }

    private BigDecimal calcularSaldoFinal(BigDecimal saldoInicial, BigDecimal totalGastado) {
        return saldoInicial.subtract(valorSeguro(totalGastado));
    }

    private BigDecimal valorSeguro(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private MesResponse toResponse(MesFinanciero mes) {
        return MesResponse.builder()
                .id(mes.getId())
                .periodo(mes.getPeriodo())
                .saldoInicial(mes.getSaldoInicial())
                .totalGastado(mes.getTotalGastado())
                .saldoFinal(mes.getSaldoFinal())
                .estado(mes.getEstado().name())
                .build();
    }
}

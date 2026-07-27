package com.mguevara.librocontable.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.mguevara.librocontable.Dto.Response.MovimientoDetalleResponse;
import com.mguevara.librocontable.Dto.Response.ReporteGlobalResponse;
import com.mguevara.librocontable.Dto.Response.ReporteMesResponse;
import com.mguevara.librocontable.Entity.MesFinanciero;
import com.mguevara.librocontable.Entity.Movimiento;
import com.mguevara.librocontable.Exception.MesException;
import com.mguevara.librocontable.Repository.MesRepository;
import com.mguevara.librocontable.Repository.MovimientoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final MesRepository mesRepository;
    private final MovimientoRepository movimientoRepository;

    @Override
    @Transactional
    public ReporteMesResponse generarReportePorMes(
            Long mesId,
            String categoria,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax) {

        validarRangos(fechaInicio, fechaFin, montoMin, montoMax);

        MesFinanciero mes = mesRepository.findById(mesId)
                .orElseThrow(() -> new MesException("Mes no encontrado con id: " + mesId, HttpStatus.NOT_FOUND));

        List<Movimiento> movimientos = movimientoRepository.buscarParaReporte(
                mesId,
                normalizarCategoria(categoria),
                fechaInicio,
                fechaFin,
                montoMin,
                montoMax
        );

        return construirReporteMes(mes, movimientos);
    }

    @Override
    @Transactional
    public ReporteGlobalResponse generarReporteGlobal(
            String categoria,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax) {

        validarRangos(fechaInicio, fechaFin, montoMin, montoMax);

        List<MesFinanciero> meses = mesRepository.findAllByOrderByPeriodoDesc();
        List<Movimiento> movimientos = movimientoRepository.buscarParaReporte(
                null,
                normalizarCategoria(categoria),
                fechaInicio,
                fechaFin,
                montoMin,
                montoMax
        );

        Map<Long, List<Movimiento>> movimientosPorMes = movimientos.stream()
                .collect(Collectors.groupingBy(movimiento -> movimiento.getMes().getId()));

        List<ReporteMesResponse> reportesMes = meses.stream()
                .map(mes -> construirReporteMes(
                        mes,
                        movimientosPorMes.getOrDefault(mes.getId(), List.of())
                ))
                .toList();

        BigDecimal saldoInicialGlobal = sumarMeses(meses, MesFinanciero::getSaldoInicial);
        BigDecimal totalGastadoGlobal = sumarMeses(meses, MesFinanciero::getTotalGastado);
        BigDecimal saldoFinalGlobal = sumarMeses(meses, MesFinanciero::getSaldoFinal);
        BigDecimal totalFiltrado = sumarMovimientos(movimientos);

        return ReporteGlobalResponse.builder()
                .saldoInicialGlobal(saldoInicialGlobal)
                .totalGastadoGlobal(totalGastadoGlobal)
                .saldoFinalGlobal(saldoFinalGlobal)
                .cantidadMeses(meses.size())
                .cantidadMovimientos(movimientos.size())
                .totalMovimientosFiltrados(totalFiltrado)
                .meses(reportesMes)
                .build();
    }

    private ReporteMesResponse construirReporteMes(MesFinanciero mes, List<Movimiento> movimientos) {
        BigDecimal totalFiltrado = sumarMovimientos(movimientos);

        return ReporteMesResponse.builder()
                .mesId(mes.getId())
                .periodo(mes.getPeriodo())
                .estado(mes.getEstado().name())
                .saldoInicial(valorSeguro(mes.getSaldoInicial()))
                .totalGastado(valorSeguro(mes.getTotalGastado()))
                .saldoFinal(valorSeguro(mes.getSaldoFinal()))
                .cantidadMovimientos(movimientos.size())
                .totalMovimientosFiltrados(totalFiltrado)
                .movimientos(movimientos.stream()
                        .map(this::toDetalle)
                        .toList())
                .build();
    }

    private MovimientoDetalleResponse toDetalle(Movimiento movimiento) {
        return MovimientoDetalleResponse.builder()
                .id(movimiento.getId())
                .mesId(movimiento.getMes().getId())
                .fechaGasto(movimiento.getFechaGasto())
                .descripcion(movimiento.getDescripcion())
                .monto(movimiento.getMonto())
                .categoria(movimiento.getCategoria())
                .build();
    }

    private void validarRangos(LocalDate fechaInicio, LocalDate fechaFin, BigDecimal montoMin, BigDecimal montoMax) {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new MesException("La fecha de inicio no puede ser posterior a la fecha fin.", HttpStatus.BAD_REQUEST);
        }

        if (montoMin != null && montoMax != null && montoMin.compareTo(montoMax) > 0) {
            throw new MesException("El monto minimo no puede ser mayor que el monto maximo.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return null;
        }

        return categoria.trim();
    }

    private BigDecimal sumarMovimientos(List<Movimiento> movimientos) {
        return movimientos.stream()
                .map(Movimiento::getMonto)
                .map(this::valorSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarMeses(List<MesFinanciero> meses, Function<MesFinanciero, BigDecimal> extractor) {
        return meses.stream()
                .map(extractor)
                .map(this::valorSeguro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorSeguro(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

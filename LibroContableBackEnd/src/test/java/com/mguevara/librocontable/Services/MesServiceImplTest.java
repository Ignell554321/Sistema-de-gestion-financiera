package com.mguevara.librocontable.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import com.mguevara.librocontable.Dto.Request.ActualizarSaldoInicialRequest;
import com.mguevara.librocontable.Dto.Request.MesRequest;
import com.mguevara.librocontable.Entity.EstadoMes;
import com.mguevara.librocontable.Entity.MesFinanciero;
import com.mguevara.librocontable.Exception.MesException;
import com.mguevara.librocontable.Repository.MesRepository;
import com.mguevara.librocontable.Repository.MovimientoRepository;

class MesServiceImplTest {

    private final MesRepository mesRepository = org.mockito.Mockito.mock(MesRepository.class);
    private final MovimientoRepository movimientoRepository = org.mockito.Mockito.mock(MovimientoRepository.class);
    private final MesServiceImpl service = new MesServiceImpl(mesRepository, movimientoRepository);

    @Test
    void crearSumaSaldoIngresadoConSaldoDisponibleDelUltimoMesCerrado() {
        MesFinanciero mesAnterior = MesFinanciero.builder()
                .id(1L)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(300))
                .saldoFinal(BigDecimal.valueOf(700))
                .estado(EstadoMes.CERRADO)
                .build();
        MesRequest request = new MesRequest();
        request.setPeriodo("2026-08");
        request.setSaldoInicial(BigDecimal.valueOf(500));
        ArgumentCaptor<MesFinanciero> captor = ArgumentCaptor.forClass(MesFinanciero.class);

        when(mesRepository.findByPeriodo("2026-08")).thenReturn(Optional.empty());
        when(mesRepository.existsByEstado(EstadoMes.ABIERTO)).thenReturn(false);
        when(mesRepository.findTopByEstadoAndPeriodoLessThanOrderByPeriodoDesc(EstadoMes.CERRADO, "2026-08"))
                .thenReturn(Optional.of(mesAnterior));
        when(mesRepository.save(org.mockito.Mockito.any(MesFinanciero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.crear(request);

        verify(mesRepository).save(captor.capture());
        assertThat(captor.getValue().getSaldoInicial()).isEqualByComparingTo("1200");
        assertThat(captor.getValue().getSaldoFinal()).isEqualByComparingTo("1200");
        assertThat(response.getSaldoInicial()).isEqualByComparingTo("1200");
    }

    @Test
    void crearUsaSaldoIngresadoCuandoNoExisteMesCerradoAnterior() {
        MesRequest request = new MesRequest();
        request.setPeriodo("2026-08");
        request.setSaldoInicial(BigDecimal.valueOf(500));
        ArgumentCaptor<MesFinanciero> captor = ArgumentCaptor.forClass(MesFinanciero.class);

        when(mesRepository.findByPeriodo("2026-08")).thenReturn(Optional.empty());
        when(mesRepository.existsByEstado(EstadoMes.ABIERTO)).thenReturn(false);
        when(mesRepository.findTopByEstadoAndPeriodoLessThanOrderByPeriodoDesc(EstadoMes.CERRADO, "2026-08"))
                .thenReturn(Optional.empty());
        when(mesRepository.save(org.mockito.Mockito.any(MesFinanciero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.crear(request);

        verify(mesRepository).save(captor.capture());
        assertThat(captor.getValue().getSaldoInicial()).isEqualByComparingTo("500");
        assertThat(captor.getValue().getSaldoFinal()).isEqualByComparingTo("500");
    }

    @Test
    void reabrirCambiaEstadoSiMesEstaCerradoYNoExisteOtroAbierto() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(250))
                .saldoFinal(BigDecimal.valueOf(750))
                .estado(EstadoMes.CERRADO)
                .build();

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));
        when(mesRepository.existsByEstadoAndIdNot(EstadoMes.ABIERTO, mesId)).thenReturn(false);
        when(mesRepository.save(mes)).thenReturn(mes);

        var response = service.reabrir(mesId);

        assertThat(response.getEstado()).isEqualTo("ABIERTO");
        verify(mesRepository).actualizarTotales(mesId);
        verify(mesRepository).save(mes);
    }

    @Test
    void reabrirRechazaSiYaExisteOtroMesAbierto() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(250))
                .saldoFinal(BigDecimal.valueOf(750))
                .estado(EstadoMes.CERRADO)
                .build();

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));
        when(mesRepository.existsByEstadoAndIdNot(EstadoMes.ABIERTO, mesId)).thenReturn(true);

        assertThatThrownBy(() -> service.reabrir(mesId))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(mesRepository, never()).save(mes);
    }

    @Test
    void reabrirRechazaSiElMesYaEstaAbierto() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.ZERO)
                .saldoFinal(BigDecimal.valueOf(1000))
                .estado(EstadoMes.ABIERTO)
                .build();

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));

        assertThatThrownBy(() -> service.reabrir(mesId))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(mesRepository, never()).existsByEstadoAndIdNot(EstadoMes.ABIERTO, mesId);
        verify(mesRepository, never()).save(mes);
    }

    @Test
    void reabrirRetornaNotFoundCuandoElMesNoExiste() {
        Long mesId = 99L;
        when(mesRepository.findById(mesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reabrir(mesId))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(mesRepository, never()).existsByEstadoAndIdNot(EstadoMes.ABIERTO, mesId);
        verify(mesRepository, never()).save(org.mockito.Mockito.any(MesFinanciero.class));
    }

    @Test
    void actualizarSaldoInicialRecalculaSaldoFinalSiMesEstaAbierto() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(500))
                .saldoFinal(BigDecimal.valueOf(500))
                .estado(EstadoMes.ABIERTO)
                .build();
        ActualizarSaldoInicialRequest request = new ActualizarSaldoInicialRequest();
        request.setSaldoInicial(BigDecimal.valueOf(1500));

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));
        when(mesRepository.save(mes)).thenReturn(mes);

        var response = service.actualizarSaldoInicial(mesId, request);

        assertThat(response.getSaldoInicial()).isEqualByComparingTo("1500");
        assertThat(response.getTotalGastado()).isEqualByComparingTo("500");
        assertThat(response.getSaldoFinal()).isEqualByComparingTo("1000");
        assertThat(response.getEstado()).isEqualTo("ABIERTO");
        verify(mesRepository).save(mes);
    }

    @Test
    void actualizarSaldoInicialRechazaMesCerrado() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(500))
                .saldoFinal(BigDecimal.valueOf(500))
                .estado(EstadoMes.CERRADO)
                .build();
        ActualizarSaldoInicialRequest request = new ActualizarSaldoInicialRequest();
        request.setSaldoInicial(BigDecimal.valueOf(1500));

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));

        assertThatThrownBy(() -> service.actualizarSaldoInicial(mesId, request))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(mesRepository, never()).save(mes);
    }

    @Test
    void actualizarSaldoInicialRetornaNotFoundCuandoElMesNoExiste() {
        Long mesId = 99L;
        ActualizarSaldoInicialRequest request = new ActualizarSaldoInicialRequest();
        request.setSaldoInicial(BigDecimal.valueOf(1500));

        when(mesRepository.findById(mesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarSaldoInicial(mesId, request))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(mesRepository, never()).save(org.mockito.Mockito.any(MesFinanciero.class));
    }

    @Test
    void eliminarBorraMovimientosAntesDelMesSinValidarEstado() {
        Long mesId = 1L;
        MesFinanciero mes = MesFinanciero.builder()
                .id(mesId)
                .periodo("2026-07")
                .saldoInicial(BigDecimal.valueOf(1000))
                .totalGastado(BigDecimal.valueOf(250))
                .saldoFinal(BigDecimal.valueOf(750))
                .estado(EstadoMes.CERRADO)
                .build();

        when(mesRepository.findById(mesId)).thenReturn(Optional.of(mes));
        when(movimientoRepository.deleteByMesId(mesId)).thenReturn(3);

        service.eliminar(mesId);

        InOrder orden = inOrder(movimientoRepository, mesRepository);
        orden.verify(movimientoRepository).deleteByMesId(mesId);
        orden.verify(mesRepository).delete(mes);
    }

    @Test
    void eliminarRetornaNotFoundCuandoElMesNoExiste() {
        Long mesId = 99L;
        when(mesRepository.findById(mesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(mesId))
                .isInstanceOfSatisfying(MesException.class, ex ->
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(movimientoRepository, never()).deleteByMesId(mesId);
        verify(mesRepository, never()).delete(org.mockito.Mockito.any(MesFinanciero.class));
    }
}

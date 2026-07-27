package com.mguevara.librocontable.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mguevara.librocontable.Entity.EstadoMes;
import com.mguevara.librocontable.Entity.MesFinanciero;

public interface MesRepository
extends JpaRepository<MesFinanciero,Long>{

    Optional<MesFinanciero> findByPeriodo(String periodo);

    Optional<MesFinanciero> findTopByEstadoOrderByPeriodoDesc(EstadoMes estado);

    Optional<MesFinanciero> findTopByEstadoAndPeriodoLessThanOrderByPeriodoDesc(EstadoMes estado, String periodo);

    List<MesFinanciero> findAllByOrderByPeriodoDesc();

    boolean existsByEstado(EstadoMes estado);

    boolean existsByEstadoAndIdNot(EstadoMes estado, Long id);

     @Modifying
    @Query(value = """
            UPDATE mes_financiero
            SET
                total_gastado = (
                    SELECT COALESCE(SUM(monto),0)
                    FROM movimiento
                    WHERE mes_id = :mesId
                ),
                saldo_final = saldo_inicial - (
                    SELECT COALESCE(SUM(monto),0)
                    FROM movimiento
                    WHERE mes_id = :mesId
                )
            WHERE id = :mesId
            """, nativeQuery = true)
    int actualizarTotales(@Param("mesId") Long mesId);
}

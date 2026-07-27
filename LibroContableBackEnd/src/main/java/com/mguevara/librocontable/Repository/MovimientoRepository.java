package com.mguevara.librocontable.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mguevara.librocontable.Entity.Movimiento;

public interface MovimientoRepository
extends JpaRepository<Movimiento,Long>{

    List<Movimiento> findByMesId(Long id);

    List<Movimiento> findByMesIdOrderByFechaGastoDescIdDesc(Long id);

    @Modifying
    @Query("""
            delete from Movimiento m
            where m.mes.id = :mesId
            """)
    int deleteByMesId(@Param("mesId") Long mesId);

    @Query("""
            select m
            from Movimiento m
            join fetch m.mes mes
            where (:mesId is null or mes.id = :mesId)
              and (:categoria is null or lower(m.categoria) = lower(:categoria))
              and (:fechaInicio is null or m.fechaGasto >= :fechaInicio)
              and (:fechaFin is null or m.fechaGasto <= :fechaFin)
              and (:montoMin is null or m.monto >= :montoMin)
              and (:montoMax is null or m.monto <= :montoMax)
            order by mes.periodo desc, m.fechaGasto desc, m.id desc
            """)
    List<Movimiento> buscarParaReporte(
            @Param("mesId") Long mesId,
            @Param("categoria") String categoria,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("montoMin") BigDecimal montoMin,
            @Param("montoMax") BigDecimal montoMax
    );

    @Query("""
    select coalesce(sum(m.monto),0)
    from Movimiento m
    where m.mes.id=:id
    """)
    BigDecimal obtenerTotal(Long id);

}

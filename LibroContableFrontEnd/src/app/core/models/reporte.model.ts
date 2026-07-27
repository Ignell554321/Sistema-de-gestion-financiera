import { Movimiento } from './movimiento.model';

export interface ReporteFiltros {
  mesId?: number;
  categoria?: string;
  fechaInicio?: string;
  fechaFin?: string;
  montoMin?: number;
  montoMax?: number;
}

export interface ReporteMes {
  mesId: number;
  periodo: string;
  estado: 'ABIERTO' | 'CERRADO' | string;
  saldoInicial: number;
  totalGastado: number;
  saldoFinal: number;
  cantidadMovimientos: number;
  totalMovimientosFiltrados: number;
  movimientos: Movimiento[];
}

export interface ReporteGlobal {
  saldoInicialGlobal: number;
  totalGastadoGlobal: number;
  saldoFinalGlobal: number;
  cantidadMeses: number;
  cantidadMovimientos: number;
  totalMovimientosFiltrados: number;
  meses: ReporteMes[];
}

export type ReporteResponse = ReporteMes | ReporteGlobal;

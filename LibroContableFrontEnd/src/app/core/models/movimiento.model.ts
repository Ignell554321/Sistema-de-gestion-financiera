export interface MovimientoRequest {
  mesId: number;
  fecha: string;
  descripcion: string;
  monto: number;
  categoria: string;
}

export interface MovimientoResponse {
  id: number;
  mesId: number;
  fechaGasto: string;
  descripcion: string;
  monto: number;
  categoria: string;
  totalGastadoMes: number;
  saldoDisponible: number;
  mensaje: string;
}

export interface Movimiento {
  id: number;
  mesId?: number;
  fechaGasto: string;
  descripcion: string;
  monto: number;
  categoria: string;
}

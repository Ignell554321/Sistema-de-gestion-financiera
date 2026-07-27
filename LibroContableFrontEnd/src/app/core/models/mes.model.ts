export interface MesRequest {
  periodo: string;
  saldoInicial: number;
}

export interface ActualizarSaldoInicialRequest {
  saldoInicial: number;
}

export interface MesResponse {
  id: number;
  periodo: string;
  saldoInicial: number;
  totalGastado: number | null;
  saldoFinal: number;
  estado: 'ABIERTO' | 'CERRADO' | string;
  cantidadMovimientos?: number | null;
  fechaCreacion?: string | null;
  fechaCierre?: string | null;
}

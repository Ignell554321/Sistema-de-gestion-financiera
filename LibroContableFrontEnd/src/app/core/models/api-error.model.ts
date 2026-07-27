export interface ApiErrorDetail {
  campo: string;
  mensaje: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message?: string;
  path: string;
  errores?: ApiErrorDetail[];
}

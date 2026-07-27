import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Movimiento, MovimientoRequest, MovimientoResponse } from '../models/movimiento.model';

@Injectable({
  providedIn: 'root'
})
export class MovimientoService {
  private readonly apiUrl = '/api/v1/movimientos';

  constructor(private readonly http: HttpClient) {}

  registrar(request: MovimientoRequest): Observable<MovimientoResponse> {
    return this.http.post<MovimientoResponse>(this.apiUrl, request);
  }

  actualizar(id: number, request: MovimientoRequest): Observable<MovimientoResponse> {
    return this.http.put<MovimientoResponse>(`${this.apiUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  listarPorMes(mesId: number): Observable<Movimiento[]> {
    return this.http.get<Movimiento[]>(this.apiUrl, {
      params: { mesId: String(mesId) }
    });
  }
}

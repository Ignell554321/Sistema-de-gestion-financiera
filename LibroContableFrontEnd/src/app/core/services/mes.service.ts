import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ActualizarSaldoInicialRequest, MesRequest, MesResponse } from '../models/mes.model';

@Injectable({
  providedIn: 'root'
})
export class MesService {
  private readonly apiUrl = '/api/v1/mes';

  constructor(private readonly http: HttpClient) {}

  crear(request: MesRequest): Observable<MesResponse> {
    return this.http.post<MesResponse>(this.apiUrl, request);
  }

  listar(): Observable<MesResponse[]> {
    return this.http.get<MesResponse[]>(this.apiUrl);
  }

  cerrar(id: number): Observable<MesResponse> {
    return this.http.patch<MesResponse>(`${this.apiUrl}/${id}/cerrar`, {});
  }

  reabrir(id: number): Observable<MesResponse> {
    return this.http.patch<MesResponse>(`${this.apiUrl}/${id}/reabrir`, {});
  }

  actualizarSaldoInicial(id: number, request: ActualizarSaldoInicialRequest): Observable<MesResponse> {
    return this.http.patch<MesResponse>(`${this.apiUrl}/${id}/saldo-inicial`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

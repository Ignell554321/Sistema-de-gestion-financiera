import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ReporteFiltros, ReporteResponse } from '../models/reporte.model';

@Injectable({
  providedIn: 'root'
})
export class ReporteService {
  private readonly apiUrl = '/api/v1/reportes';

  constructor(private readonly http: HttpClient) {}

  generar(filtros: ReporteFiltros): Observable<ReporteResponse> {
    let params = new HttpParams();

    Object.entries(filtros).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<ReporteResponse>(this.apiUrl, { params });
  }
}

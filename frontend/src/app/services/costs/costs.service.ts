import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CostLine } from '../../models/cost.model';

@Injectable({ providedIn: 'root' })
export class CostsService {
  private http = inject(HttpClient);
  private readonly API = `${environment.apiBaseUrl}/costs`;

  list(filters: { q?: string; buId?: string; status?: string } = {}): Observable<CostLine[]> {
    let params = new HttpParams();
    if (filters.q?.trim())      params = params.set('q', filters.q.trim());
    if (filters.buId?.trim())   params = params.set('buId', filters.buId.trim());
    if (filters.status?.trim()) params = params.set('status', filters.status.trim());
    return this.http.get<CostLine[]>(this.API, { params });
  }
}
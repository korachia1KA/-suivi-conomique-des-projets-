import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmployeeCard } from '../../models/employee.model';

/**
 * Employees listing service.
 * Uses the real GET /user/list endpoint (backend scopes by role).
 */
@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);
  private readonly API = `${environment.apiBaseUrl}/user/list`;

  list(params?: { q?: string; buId?: string; role?: string }): Observable<EmployeeCard[]> {
    let httpParams = new HttpParams();

    if (params?.q?.trim())    httpParams = httpParams.set('q',    params.q.trim());
    if (params?.buId?.trim()) httpParams = httpParams.set('buId', params.buId.trim());
    if (params?.role?.trim()) httpParams = httpParams.set('role', params.role.trim());

    return this.http
      .get<any[]>(this.API, { params: httpParams })
      .pipe(
        map((rows) =>
          (rows ?? []).map((row) => ({
            id:             String(row?.idUser ?? ''),
            idUser:         String(row?.idUser ?? ''),
            firstName:      row?.firstName ?? '',
            lastName:       row?.lastName ?? '',
            name:           `${row?.firstName ?? ''} ${row?.lastName ?? ''}`.trim(),
            email:          row?.email ?? '',
            role:           row?.role ?? '',
            seniorityLevel: row?.seniorityLevel ?? '',
            businessUnitId: row?.businessUnitId ?? '',
            businessUnit:   row?.businessUnit ?? '',
            businessUnitName: row?.businessUnit ?? '',
          }))
        )
      );
  }
}
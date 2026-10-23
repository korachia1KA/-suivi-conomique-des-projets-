import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminUser } from '../../models/adminUser.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private http = inject(HttpClient);
  private readonly API = `${environment.apiBaseUrl}/user`;

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.API}/admin/list`);
  }

  assignBusinessUnit(idUser: string, businessUnitId: string | null): Observable<void> {
    return this.http.put<void>(
      `${this.API}/${idUser}/business-unit`,
      { businessUnitId }
    );
  }
}
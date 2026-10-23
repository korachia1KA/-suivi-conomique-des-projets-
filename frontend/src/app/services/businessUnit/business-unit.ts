import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { BusinessUnitOption } from '../../models/businessUnitOption.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class BusinessUnitService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private readonly API_URL = `${environment.apiBaseUrl}/business-units`;

  list(): Observable<BusinessUnitOption[]> {
    if (!isPlatformBrowser(this.platformId)) {
      return of([]); 
    }
    return this.http.get<any[]>(this.API_URL).pipe(
      map((rows) =>
        (rows ?? [])
          .map((row) => ({
            id: String(row?.idBusinessUnit ?? row?.id ?? ''),
            name: String(row?.name ?? ''),
            currentAverageMargin:
              row?.currentAverageMargin ??
              row?.current_average_margin ??
              row?.averageMargin ??
              row?.avgMargin ??
              null,
            employeeCount:
              row?.employeeCount ??
              row?.employeesCount ??
              row?.numberOfEmployees ??
              row?.employee_count ??
              null,
            managerName:
              row?.managerName ??
              row?.managerFullName ??
              (row?.managerFirstName && row?.managerLastName
                ? `${row.managerFirstName} ${row.managerLastName}`
                : null),
          }))
          .filter((row) => row.id !== '' && row.name !== '')
      )
    );
  }
}
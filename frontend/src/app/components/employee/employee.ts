import {
  Component, DestroyRef, OnInit, PLATFORM_ID,
  computed, inject, signal,
} from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatCard } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

import { EmployeeService } from '../../services/employee/employee';
import { AuthService } from '../../services/auth/auth.service';
import { UserService } from '../../services/user/user';
import { EmployeeCard } from '../../models/employee.model';

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [
    MatCard, MatButtonModule, MatIconModule, MatInputModule, MatFormFieldModule,
    FormsModule, DatePipe, TranslateModule,
  ],
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class EmployeesComponent implements OnInit {
  private employeeService = inject(EmployeeService);
  private authService     = inject(AuthService);
  private translate       = inject(TranslateService);
  userService             = inject(UserService);
  private destroyRef      = inject(DestroyRef);
  private platformId      = inject(PLATFORM_ID);

  loading      = signal(true);
  error        = signal<string | null>(null);
  rawEmployees = signal<EmployeeCard[]>([]);
  search       = signal('');
  buFilter     = signal('');
  roleFilter   = signal('');

  pipeLocale = this.mapLocale(this.translate.getCurrentLang());
  private search$ = new Subject<string>();

  get isManager(): boolean {
    if (this.authService.hasRole('MANAGER')) return true;
    const profileRole = String(
      this.userService.currentUser()?.roleName ??
      this.userService.currentUser()?.role?.name ?? ''
    ).toUpperCase();
    return profileRole.includes('MANAGER');
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) { this.loading.set(false); return; }

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => (this.pipeLocale = this.mapLocale(e.lang)));

    this.search$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(v => { this.search.set(v); this.loadEmployees(); });

    this.loadEmployees();
  }

  private loadEmployees(): void {
    this.loading.set(true);
    this.error.set(null);

    this.employeeService
      .list({
        q: this.search() || undefined,
        buId: this.buFilter() || undefined,
        role: this.roleFilter() || undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => { this.rawEmployees.set(list ?? []); this.loading.set(false); },
        error: (err: HttpErrorResponse) => {
          console.error('Employees load error', err);
          if (err.status === 401) this.authService.logout();
          const msg = typeof err.error?.message === 'string' ? err.error.message : null;
          this.error.set(msg || `Erreur chargement employés (${err.status || 0})`);
          this.loading.set(false);
        },
      });
  }

  onSearch(v: string)   { this.search$.next(v ?? ''); }
  onBuFilter(v: string) { this.buFilter.set(v ?? '');   this.loadEmployees(); }
  onRoleFilter(v: string){ this.roleFilter.set(v ?? ''); this.loadEmployees(); }

  // ---- Helpers ----
  employeeId(e: EmployeeCard): string {
    return String(e?.idUser ?? e?.id ?? this.employeeEmail(e));
  }
  employeeDisplayName(item: EmployeeCard): string {
    const direct = String(item?.name ?? '').trim();
    if (direct) return direct;
    const first = String(item?.firstName ?? '').trim();
    const last  = String(item?.lastName  ?? '').trim();
    const composed = `${first} ${last}`.trim();
    if (composed) return composed;
    const email = this.employeeEmail(item);
    if (email !== '-') {
      const alias = email.split('@')[0]?.replace(/[._-]+/g, ' ').trim();
      if (alias) return alias.replace(/\b\w/g, c => c.toUpperCase());
    }
    return this.translate.instant('EMPLOYEES.UNKNOWN') || 'Utilisateur';
  }
  employeeEmail(item: EmployeeCard): string {
    const raw = item as any;
    return String(item?.email ?? raw?.mail ?? raw?.userEmail ?? '').trim() || '-';
  }
  employeeRole(item: EmployeeCard): string {
    return String(item?.role ?? '-').trim() || '-';
  }
  employeeBU(item: EmployeeCard): string {
    return String(item?.businessUnitName ?? item?.businessUnit ?? '-').trim() || '-';
  }
  employeeSeniority(item: EmployeeCard): string {
    return String(item?.seniorityLevel ?? '-').trim() || '-';
  }

  get uniqueBusinessUnits(): string[] {
    const s = new Set<string>();
    this.rawEmployees().forEach(e => {
      const v = this.employeeBU(e);
      if (v && v !== '-') s.add(v);
    });
    return Array.from(s).sort();
  }
  get uniqueRoles(): string[] {
    const s = new Set<string>();
    this.rawEmployees().forEach(e => {
      const v = this.employeeRole(e);
      if (v && v !== '-') s.add(v);
    });
    return Array.from(s).sort();
  }

  private mapLocale(lang: string | undefined) {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }
}
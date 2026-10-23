import { Component, DestroyRef, inject, PLATFORM_ID, signal } from '@angular/core';
import { AsyncPipe, DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { RealtimeService } from '../../services/realtime/realtime';

import { MatIcon } from '@angular/material/icon';
import { MatCard } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, DateAdapter } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';

import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import {
  catchError, combineLatest, debounceTime, distinctUntilChanged, filter, map,
  Observable, of, shareReplay, switchMap, tap,
} from 'rxjs';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { ProjectsService } from '../../services/projects/projects';
import { BusinessUnitService } from '../../services/businessUnit/business-unit';
import { AuthService } from '../../services/auth/auth.service';
import { UserService } from '../../services/user/user';

import { ProjectRow } from '../../models/projectRow.model';
import { UserProfile } from '../../models/userProfile.model';

type SortState = { field: string; dir: 'asc' | 'desc' };

@Component({
  standalone: true,
  selector: 'app-projects',
  imports: [
    AsyncPipe,
    DatePipe,
    DecimalPipe,
    MatIcon,
    MatCard,
    MatTableModule,
    MatSortModule,
    MatMenuModule,
    MatButtonModule,
    MatSelectModule,
    MatSliderModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    RouterLink,
    FormsModule,
    TranslateModule,
  ],
  templateUrl: './projects.html',
  styleUrl: './projects.scss',
})
export class ProjectsComponent {
  private static readonly DEFAULT_MARGIN_MIN = -40;
  private static readonly DEFAULT_MARGIN_MAX = 100;

  private translate = inject(TranslateService);
  private projectService = inject(ProjectsService);
  private buService = inject(BusinessUnitService);
  private authService = inject(AuthService);
  private userService = inject(UserService);

  private platformId = inject(PLATFORM_ID);
  private dateAdapter = inject(DateAdapter);
  private destroyRef = inject(DestroyRef);

  private realtime = inject(RealtimeService);

  reloadSig = signal(0);

  loading = signal(false);
  errorMessage = signal<string | null>(null);

  sortSig = signal<SortState>({ field: 'name', dir: 'asc' });

  pageSig = signal(0);
  sizeSig = signal(10);
  totalSig = signal(0);
  globalProjectsCountSig = signal(0);
  isManagerSig = signal(false);
  isAdminSig = signal(false);
  managerBuIdSig = signal('');

  searchDraftSig = signal<string>('');

  buDraftSig = signal<string>('');
  statusDraftSig = signal<string>('');
  startFromDraftSig = signal<string>('');
  startToDraftSig = signal<string>('');

  marginMinSig = signal<number>(ProjectsComponent.DEFAULT_MARGIN_MIN);
  marginMaxSig = signal<number>(ProjectsComponent.DEFAULT_MARGIN_MAX);

  private skipResetOnClose = false;

  filtersSig = signal<{
    bu: string;
    status: string;
    startFrom: string;
    startTo: string;
    marginMin: number | null;
    marginMax: number | null;
  }>({
    bu: '',
    status: '',
    startFrom: '',
    startTo: '',
    marginMin: null,
    marginMax: null,
  });

  pipeLocale = this.mapLocale(this.translate.getCurrentLang());
  private readonly adminColumns = ['name', 'bu', 'start', 'end', 'revenue', 'cost', 'margin'];
  private readonly managerColumns = ['name', 'start', 'end', 'revenue', 'cost', 'margin'];

  userProfile$ = toObservable(this.authService.currentUserSig).pipe(
    filter(Boolean),
    switchMap(() =>
      this.userService.getMe().pipe(
        catchError(() => of(null as UserProfile | null))
      )
    ),
    shareReplay(1)
  );

  constructor() {
    const locale = this.mapLocale(this.translate.getCurrentLang());
    this.pipeLocale = locale;
    this.dateAdapter.setLocale(locale);

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => {
        const next = this.mapLocale(e.lang);
        this.pipeLocale = next;
        this.dateAdapter.setLocale(next);
      });

    // ---- Total count for the "count" badge ----
    toObservable(this.authService.currentUserSig)
      .pipe(
        filter(Boolean),
        switchMap(() =>
          this.projectService.totals('', 'name', 'asc', {}).pipe(
            map(res => Number(res.count ?? 0)),
            catchError(() => of(0))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(count => this.globalProjectsCountSig.set(count));

    // ---- Role detection + WS subscriptions ----
    this.userProfile$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(profile => {
        const role = this.extractRole(profile);
        const buId = this.extractBusinessUnitId(profile);
        const isAdmin =
          this.authService.hasRole('ADMIN') ||
          this.isAdminRole(role) ||
          this.isAdminFromToken();
        const isManager =
          !isAdmin && (
            this.authService.hasRole('MANAGER') ||
            this.isManagerRole(role) ||
            this.isManagerFromToken()
          );

        this.isAdminSig.set(isAdmin);
        this.isManagerSig.set(isManager);
        this.managerBuIdSig.set(isManager ? buId : '');

        if (isManager && buId) {
          this.buDraftSig.set(buId);
          this.filtersSig.update(f => ({ ...f, bu: buId }));
        }

        if (isAdmin) {
          this.realtime.watchAllProjectsCreated();
          this.realtime.watchAllProjectsCritical();
          this.realtime.watchAllProjectsUpdated();
          return;
        }
        if (isManager && buId) {
          this.realtime.watchBuProjectsCreated(buId);
          this.realtime.watchBuProjectsCritical(buId);
          this.realtime.watchBuProjectsUpdated(buId);
        }
      });

    // ---- Live refresh: any of the three event kinds triggers a reload ----
    this.realtime.projectCreated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reloadSig.update(v => v + 1));

    this.realtime.projectCritical$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reloadSig.update(v => v + 1));

    this.realtime.projectUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reloadSig.update(v => v + 1));
  }

  get displayedColumns(): string[] {
    return this.isAdminSig() ? this.adminColumns : this.managerColumns;
  }

  get currentLang(): string {
    return this.translate.getCurrentLang() || 'it';
  }

  setLang(lang: string) {
    this.translate.use(lang);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('lang', lang);
    }
  }

  private mapLocale(lang: string | undefined) {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }

  businessUnits$ = toObservable(this.authService.currentUserSig).pipe(
    filter(Boolean),
    switchMap(() => this.buService.list()),
    shareReplay(1)
  );

  private parseLocalDate(value: string): Date | null {
    if (!value) return null;
    const [y, m, d] = value.split('-').map(Number);
    if (!y || !m || !d) return null;
    return new Date(y, m - 1, d);
  }

  private formatLocalDate(date: Date | null): string {
    if (!date) return '';
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  get startFromDate(): Date | null {
    return this.parseLocalDate(this.startFromDraftSig());
  }

  onStartFromDateChange(value: Date | null) {
    this.startFromDraftSig.set(this.formatLocalDate(value));
  }

  private search$ = toObservable(this.searchDraftSig).pipe(
    map(v => v.trim()),
    debounceTime(550),
    map(v => (v.length >= 3 ? v : '')),
    distinctUntilChanged(),
    shareReplay(1)
  );

  projects$: Observable<ProjectRow[]> = combineLatest([
    toObservable(this.authService.currentUserSig).pipe(filter(Boolean)),
    toObservable(this.sortSig),
    this.search$,
    toObservable(this.filtersSig),
    toObservable(this.isManagerSig),
    toObservable(this.managerBuIdSig),
    toObservable(this.pageSig),
    toObservable(this.sizeSig),
    toObservable(this.reloadSig),
  ]).pipe(
    tap(() => {
      this.loading.set(true);
      this.errorMessage.set(null);
    }),
    switchMap(([_, sort, q, filters, isManager, managerBuId, page, size, _reload]) =>
      this.projectService.list(q, sort.field, sort.dir, {
        bu: isManager ? managerBuId : filters.bu,
        status: filters.status,
        startFrom: filters.startFrom,
        startTo: filters.startTo,
        marginMin: filters.marginMin,
        marginMax: filters.marginMax,
        page,
        size,
      }).pipe(
        tap(res => { this.totalSig.set(res.total); }),
        map(res => res.items),
        tap(() => this.loading.set(false)),
        catchError(() => {
          this.loading.set(false);
          this.errorMessage.set('PROJECT.ERROR.LOAD');
          return of([]);
        })
      )
    ),
    shareReplay(1)
  );

  totals$ = combineLatest([
    toObservable(this.authService.currentUserSig).pipe(filter(Boolean)),
    toObservable(this.sortSig),
    this.search$,
    toObservable(this.filtersSig),
    toObservable(this.isManagerSig),
    toObservable(this.managerBuIdSig),
    toObservable(this.reloadSig),
  ]).pipe(
    switchMap(([_, sort, q, filters, isManager, managerBuId, _reload]) =>
      this.projectService.totals(q, sort.field, sort.dir, {
        bu: isManager ? managerBuId : filters.bu,
        status: filters.status,
        startFrom: filters.startFrom,
        startTo: filters.startTo,
        marginMin: filters.marginMin,
        marginMax: filters.marginMax,
      }).pipe(
        map(res => ({
          count: Number(res.count ?? 0),
          totalRevenue: Number(res.totalRevenue ?? 0),
          totalCost: Number(res.totalCost ?? 0),
          marginPct: Number(res.marginPct ?? 0),
        })),
        catchError(() => of({
          count: 0,
          totalRevenue: 0,
          totalCost: 0,
          marginPct: 0,
        }))
      )
    ),
    shareReplay(1)
  );

  onSortChange(e: Sort) {
    const field = e.active || 'name';
    const dir = (e.direction || 'asc') as 'asc' | 'desc';
    this.pageSig.set(0);
    this.sortSig.set({ field, dir });
  }

  onSearch(value: string) {
    this.pageSig.set(0);
    this.searchDraftSig.set(value);
  }

  clearSearch() {
    this.pageSig.set(0);
    this.searchDraftSig.set('');
  }

  onPageChange(e: PageEvent) {
    this.pageSig.set(e.pageIndex);
    this.sizeSig.set(e.pageSize);
  }

  onBuChange(value: string | number | null) {
    this.buDraftSig.set(value == null ? '' : String(value));
  }

  onMarginMinChange(value: number) {
    const max = this.marginMaxSig();
    this.marginMinSig.set(Math.min(value, max));
  }

  onMarginMaxChange(value: number) {
    const min = this.marginMinSig();
    this.marginMaxSig.set(Math.max(value, min));
  }

  applyFilters() {
    this.pageSig.set(0);
    this.skipResetOnClose = true;

    const marginMinRaw = this.marginMinSig();
    const marginMaxRaw = this.marginMaxSig();

    const useMargin =
      !(marginMinRaw === ProjectsComponent.DEFAULT_MARGIN_MIN &&
        marginMaxRaw === ProjectsComponent.DEFAULT_MARGIN_MAX);

    this.filtersSig.set({
      bu: this.buDraftSig(),
      status: this.statusDraftSig(),
      startFrom: this.startFromDraftSig(),
      startTo: this.startToDraftSig(),
      marginMin: useMargin ? marginMinRaw : null,
      marginMax: useMargin ? marginMaxRaw : null,
    });
  }

  resetFilters() {
    this.pageSig.set(0);
    this.buDraftSig.set('');
    this.statusDraftSig.set('');
    this.startFromDraftSig.set('');
    this.startToDraftSig.set('');
    this.marginMinSig.set(ProjectsComponent.DEFAULT_MARGIN_MIN);
    this.marginMaxSig.set(ProjectsComponent.DEFAULT_MARGIN_MAX);
    this.applyFilters();
  }

  onFiltersMenuClosed() {
    if (this.skipResetOnClose) {
      this.skipResetOnClose = false;
      return;
    }
    const f = this.filtersSig();
    this.buDraftSig.set(f.bu);
    this.statusDraftSig.set(f.status);
    this.startFromDraftSig.set(f.startFrom);
    this.startToDraftSig.set(f.startTo);
    this.marginMinSig.set(f.marginMin ?? ProjectsComponent.DEFAULT_MARGIN_MIN);
    this.marginMaxSig.set(f.marginMax ?? ProjectsComponent.DEFAULT_MARGIN_MAX);
  }

  private extractRole(profile: UserProfile | null): string {
    const p = profile as unknown as Record<string, unknown> | null;
    const roleRaw = p?.['role'];
    const roleName =
      profile?.roleName ??
      (typeof roleRaw === 'string'
        ? roleRaw
        : (typeof roleRaw === 'object' && roleRaw !== null
            ? String((roleRaw as Record<string, unknown>)['name'] ?? '')
            : ''));
    return String(roleName ?? '').toUpperCase();
  }

  private extractBusinessUnitId(profile: UserProfile | null): string {
    return String(profile?.businessUnitId ?? profile?.businessUnit?.idBusinessUnit ?? '');
  }

  private isManagerRole(role: string): boolean {
    return role.includes('MANAGER');
  }

  private isAdminRole(role: string): boolean {
    return role.includes('ADMIN');
  }

  private isManagerFromToken(): boolean {
    try {
      if (!isPlatformBrowser(this.platformId)) return false;
      const token = this.authService.getAccessToken();
      if (!token) return false;
      const payloadB64 = token.split('.')[1];
      if (!payloadB64) return false;
      const normalized = payloadB64.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
      const payload = JSON.parse(atob(padded)) as Record<string, unknown>;

      const role = String(payload['role'] ?? payload['authorities'] ?? payload['roles'] ?? '').toUpperCase();
      if (role.includes('MANAGER')) return true;

      const authorities = payload['authorities'];
      if (Array.isArray(authorities)) {
        return authorities.some(a => {
          const raw = typeof a === 'object' && a !== null
            ? String((a as Record<string, unknown>)['authority'] ?? '')
            : String(a);
          return raw.toUpperCase().includes('MANAGER');
        });
      }

      const roles = payload['roles'];
      if (Array.isArray(roles)) {
        return roles.some(r => {
          const raw = typeof r === 'object' && r !== null
            ? String((r as Record<string, unknown>)['name'] ?? (r as Record<string, unknown>)['authority'] ?? '')
            : String(r);
          return raw.toUpperCase().includes('MANAGER');
        });
      }
    } catch {
      return false;
    }
    return false;
  }

  private isAdminFromToken(): boolean {
    try {
      if (!isPlatformBrowser(this.platformId)) return false;
      const token = this.authService.getAccessToken();
      if (!token) return false;
      const payloadB64 = token.split('.')[1];
      if (!payloadB64) return false;
      const normalized = payloadB64.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
      const payload = JSON.parse(atob(padded)) as Record<string, unknown>;

      const role = String(payload['role'] ?? payload['authorities'] ?? payload['roles'] ?? '').toUpperCase();
      if (role.includes('ADMIN')) return true;

      const authorities = payload['authorities'];
      if (Array.isArray(authorities)) {
        return authorities.some(a => {
          const raw = typeof a === 'object' && a !== null
            ? String((a as Record<string, unknown>)['authority'] ?? '')
            : String(a);
          return raw.toUpperCase().includes('ADMIN');
        });
      }

      const roles = payload['roles'];
      if (Array.isArray(roles)) {
        return roles.some(r => {
          const raw = typeof r === 'object' && r !== null
            ? String((r as Record<string, unknown>)['name'] ?? (r as Record<string, unknown>)['authority'] ?? '')
            : String(r);
          return raw.toUpperCase().includes('ADMIN');
        });
      }
    } catch {
      return false;
    }
    return false;
  }
}
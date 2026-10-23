import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCard } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { DashboardService } from '../../services/dashboard/dashboard';
import { DashboardResponse, BusinessUnitCard, EmployeeCard, ProjectCard } from '../../models/dashboard.model';
import { AuthService } from '../../services/auth/auth.service';
import { UserService } from '../../services/user/user';
import { RealtimeService } from '../../services/realtime/realtime';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCard, MatButtonModule, RouterLink, DecimalPipe, DatePipe, TranslateModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  userService = inject(UserService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  private realtime = inject(RealtimeService);

  loading = signal(true);
  error = signal<string | null>(null);
  dashboard = signal<DashboardResponse | null>(null);

  pipeLocale = this.mapLocale(this.translate.getCurrentLang());
  Math: any;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => (this.pipeLocale = this.mapLocale(e.lang)));

    this.userService.loadProfile();
    this.loadDashboard();

    // Subscribe to WS topics based on role
    this.userService.getMe()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(profile => {
        const buId = String(profile?.businessUnitId ?? profile?.businessUnit?.idBusinessUnit ?? '').trim();

        if (this.authService.hasRole('ADMIN')) {
          this.realtime.watchAllProjectsCreated();
          this.realtime.watchAllProjectsCritical();
          this.realtime.watchAllProjectsUpdated();
        } else if (this.authService.hasRole('MANAGER') && buId) {
          this.realtime.watchBuProjectsCreated(buId);
          this.realtime.watchBuProjectsCritical(buId);
          this.realtime.watchBuProjectsUpdated(buId);
        }
      });

    // Live refresh: any of the three event kinds triggers a dashboard reload
    this.realtime.projectCreated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDashboard());

    this.realtime.projectCritical$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDashboard());

    this.realtime.projectUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDashboard());
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService
      .getDashboard()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.dashboard.set(res);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Dashboard load error', err);
          if (err.status === 401) {
            this.authService.logout();
          }
          const backendMsg =
            typeof err.error?.message === 'string' ? err.error.message : null;
          this.error.set(backendMsg || `Errore nel caricamento dashboard (${err.status || 0})`);
          this.loading.set(false);
        }
      });
  }

  private mapLocale(lang: string | undefined) {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }

  get role(): 'ADMIN' | 'MANAGER' | 'EMPLOYEE' {
    if (this.authService.hasRole('MANAGER')) return 'MANAGER';
    if (this.authService.hasRole('ADMIN')) return 'ADMIN';

    const profileRole = String(
      this.userService.currentUser()?.roleName ??
      this.userService.currentUser()?.role?.name ??
      ''
    ).toUpperCase();
    if (profileRole.includes('MANAGER')) return 'MANAGER';
    if (profileRole.includes('ADMIN')) return 'ADMIN';

    const dashboardRole = String(this.dashboard()?.userRole ?? '').toUpperCase();
    if (dashboardRole.includes('MANAGER')) return 'MANAGER';
    if (dashboardRole.includes('ADMIN')) return 'ADMIN';

    return 'EMPLOYEE';
  }

  get isAdmin(): boolean { return this.role === 'ADMIN'; }
  get isManager(): boolean { return this.role === 'MANAGER'; }
  get isManagerEmployeeView(): boolean {
    const managerFromAuth = this.authService.hasRole('MANAGER');
    const adminFromAuth = this.authService.hasRole('ADMIN');
    return this.isManager || (managerFromAuth && !adminFromAuth);
  }

  get totalProjectsCount(): number { return this.dashboard()?.totalProjects ?? 0; }
  get activeProjectsCount(): number { return this.dashboard()?.activeProjects ?? 0; }
  get plannedProjectsCount(): number { return this.dashboard()?.scheduledProjects ?? 0; }

  get totalRevenueAmount(): number { return this.dashboard()?.totalRevenue ?? 0; }
  get totalCostAmount(): number { return this.dashboard()?.totalCost ?? 0; }
  get averageMarginPct(): number { return this.dashboard()?.averageMargin ?? 0; }

  get marginStatus(): 'ok' | 'warning' | 'danger' {
    const m = this.averageMarginPct;
    if (m >= 25) return 'ok';
    if (m >= 15) return 'warning';
    return 'danger';
  }

  get topProjectsByMargin(): Array<{ label: string; margin: number; norm: number }> {
    const items = this.dashboard()?.topProjects ?? [];
    return items.map(x => ({
      label: x.label,
      margin: Number(x.value ?? 0),
      norm: Math.max(0, Math.min(100, Number(x.value ?? 0))),
    }));
  }

  get topBuByMargin(): Array<{ label: string; margin: number; norm: number }> {
    const items = this.dashboard()?.topBusinessUnits ?? [];
    return items.map(x => ({
      label: x.label,
      margin: Number(x.value ?? 0),
      norm: Math.max(0, Math.min(100, Number(x.value ?? 0))),
    }));
  }

  get projectOkCount(): number { return this.dashboard()?.projectRisk?.ok ?? 0; }
  get projectWarnCount(): number { return this.dashboard()?.projectRisk?.warning ?? 0; }
  get projectKoCount(): number { return this.dashboard()?.projectRisk?.critical ?? 0; }
  get projectTotalCount(): number { return this.projectOkCount + this.projectWarnCount + this.projectKoCount; }

  get buOkCount(): number { return this.dashboard()?.businessUnitRisk?.ok ?? 0; }
  get buWarnCount(): number { return this.dashboard()?.businessUnitRisk?.warning ?? 0; }
  get buKoCount(): number { return this.dashboard()?.businessUnitRisk?.critical ?? 0; }
  get buTotalCount(): number { return this.buOkCount + this.buWarnCount + this.buKoCount; }

  get projectMarginPieBackground(): string {
    return this.buildPieFromBuckets(this.projectOkCount, this.projectWarnCount, this.projectKoCount);
  }

  get buMarginPieBackground(): string {
    return this.buildPieFromBuckets(this.buOkCount, this.buWarnCount, this.buKoCount);
  }

  get businessUnitsForCard(): BusinessUnitCard[] {
    return this.dashboard()?.businessUnits ?? [];
  }

  get employeesForCard(): EmployeeCard[] {
    return this.dashboard()?.employees ?? [];
  }

  get comparisonRows(): Array<{
    key: 'revenue' | 'cost' | 'margin';
    expected: number;
    current: number;
    unit: 'currency' | 'percent';
    goodWhenLower: boolean;
  }> {
    const d = this.dashboard();
    return [
      {
        key: 'revenue',
        expected: Number(d?.expectedRevenue ?? 0),
        current: this.totalRevenueAmount,
        unit: 'currency',
        goodWhenLower: false,
      },
      {
        key: 'cost',
        expected: Number(d?.expectedCost ?? 0),
        current: this.totalCostAmount,
        unit: 'currency',
        goodWhenLower: true,
      },
      {
        key: 'margin',
        expected: Number(d?.expectedMargin ?? 0),
        current: this.averageMarginPct,
        unit: 'percent',
        goodWhenLower: false,
      },
    ];
  }

  get projectsForCard() {
    return (this.dashboard()?.projects ?? []).slice(0, 4);
  }

  projectBusinessUnit(project: ProjectCard): string {
    return String(project?.businessUnitName ?? project?.businessUnit ?? '').trim() || '-';
  }

  projectStatusLabel(status?: string | null): string {
    const normalized = String(status ?? '').trim().toUpperCase();
    if (!normalized) return '-';
    const key = `PROJECT.STATUS.${normalized}`;
    const translated = this.translate.instant(key);
    if (translated && translated !== key) return translated;
    return normalized
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  toPct(value: number, total: number): number {
    if (!total) return 0;
    return (Math.max(0, value) / total) * 100;
  }

  metricScaleMaxByKey(key: 'revenue' | 'cost' | 'margin'): number {
    if (key === 'margin') return 100;
    return this.isManager ? 250_000 : 1_000_000;
  }

  metricPosByKey(value: number, key: 'revenue' | 'cost' | 'margin'): number {
    const max = this.metricScaleMaxByKey(key);
    const pct = (Math.max(0, value) / Math.max(1, max)) * 100;
    return Math.max(0, Math.min(100, pct));
  }

  readonly moneyAxisTicksAdmin = [0, 250000, 500000, 750000, 1000000];
  readonly moneyAxisTicksManager = [0, 62500, 125000, 187500, 250000];
  readonly marginAxisTicks = [0, 25, 50, 75, 100];

  get moneyAxisTicks(): number[] {
    return this.isManager ? this.moneyAxisTicksManager : this.moneyAxisTicksAdmin;
  }

  axisTicksByKey(key: 'revenue' | 'cost' | 'margin'): number[] {
    return key === 'margin' ? this.marginAxisTicks : this.moneyAxisTicks;
  }

  axisTickLabel(tick: number, unit: 'currency' | 'percent'): string {
    if (unit === 'percent') return `${tick}%`;
    if (tick === 0) return '0€';
    if (tick >= 1_000_000) return '1M€';
    return `${Math.round(tick / 1000)}K€`;
  }

  metricDelta(expected: number, current: number): number {
    return current - expected;
  }

  metricDeltaClass(expected: number, current: number, goodWhenLower: boolean): 'up' | 'down' | 'flat' {
    const delta = current - expected;
    if (Math.abs(delta) < 0.0001) return 'flat';
    if (goodWhenLower) return delta <= 0 ? 'up' : 'down';
    return delta >= 0 ? 'up' : 'down';
  }

  buMarginStatusValue(margin: number | null | undefined): 'ok' | 'warning' | 'danger' | 'none' {
    if (margin == null) return 'none';
    if (margin >= 25) return 'ok';
    if (margin >= 15) return 'warning';
    return 'danger';
  }

  private buildPieFromBuckets(ok: number, warn: number, ko: number): string {
    const total = ok + warn + ko;
    if (!total) return 'conic-gradient(#d7e0e6 0 100%)';

    const okPct = (ok / total) * 100;
    const warnPct = (warn / total) * 100;
    const koPct = (ko / total) * 100;

    const s1 = okPct;
    const s2 = okPct + warnPct;
    const s3 = okPct + warnPct + koPct;

    return `conic-gradient(
      #2e7d32 0 ${s1}%,
      #d9a400 ${s1}% ${s2}%,
      #d32f2f ${s2}% ${s3}%
    )`;
  }

  get upcomingDeadlines() {
    return (this.dashboard()?.upcomingDeadlines ?? []).filter(item => item.days <= 30);
  }

  employeeDisplayName(item: EmployeeCard): string {
    const direct = String(item?.name ?? '').trim();
    if (direct) return direct;

    const raw = item as any;
    const displayName = String(raw?.displayName ?? '').trim();
    if (displayName) return displayName;

    const fullName = String(raw?.fullName ?? '').trim();
    if (fullName) return fullName;

    const userName = String(raw?.userName ?? raw?.username ?? '').trim();
    if (userName) return userName;

    const firstName = String(raw?.firstName ?? '').trim();
    const lastName = String(raw?.lastName ?? '').trim();
    const composed = `${firstName} ${lastName}`.trim();
    if (composed) return composed;

    const email = this.employeeEmail(item);
    if (email !== '-') {
      const alias = email.split('@')[0]?.replace(/[._-]+/g, ' ').trim();
      if (alias) {
        return alias.replace(/\b\w/g, c => c.toUpperCase());
      }
    }

    return 'Utente';
  }

  employeeEmail(item: EmployeeCard): string {
    const raw = item as any;
    return String(item?.email ?? raw?.mail ?? raw?.userEmail ?? '').trim() || '-';
  }

  get overdueCount(): number {
    return this.dashboard()?.deadlineCounts?.overdue ?? 0;
  }

  get due7Count(): number {
    const dc: any = this.dashboard()?.deadlineCounts;
    return dc?.due7 ?? dc?.next7 ?? 0;
  }

  get due30Count(): number {
    const dc: any = this.dashboard()?.deadlineCounts;
    return dc?.due30 ?? dc?.next30 ?? 0;
  }

  get showManagerDeadlineColumn(): boolean {
    if (!this.isManager) return false;
    return (this.overdueCount + this.due7Count + this.due30Count) > 0;
  }

  deadlinePillClass(days: number): 'soon' | 'warning' | 'danger' {
    if (days < 0) return 'danger';
    if (days <= 7) return 'soon';
    return 'warning';
  }

  deadlineLabel(days: number): string {
    if (days < 0) {
      return this.translate.instant('HOME.DEADLINES.LABEL_OVERDUE', { days: Math.abs(days) });
    }
    if (days === 0) {
      return this.translate.instant('HOME.DEADLINES.LABEL_TODAY');
    }
    return this.translate.instant('HOME.DEADLINES.LABEL_DAYS', { days });
  }

  deadlineMainLabel(days: number): string {
    return '';
  }

  deadlineDaysText(days: number): string {
    return this.translate.instant('HOME.DEADLINES.LABEL_DAYS', { days: Math.abs(days) });
  }
}
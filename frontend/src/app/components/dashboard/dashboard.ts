import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DashboardService } from '../../services/dashboard/dashboard';
import { DashboardResponse, BusinessUnitCard, EmployeeCard, ProjectCard } from '../../models/dashboard.model';
import { AuthService } from '../../services/auth/auth.service';
import { UserService } from '../../services/user/user';
import { RealtimeService } from '../../services/realtime/realtime';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    // Angular Material
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,

    // Angular core
    DecimalPipe,
    DatePipe,
    RouterLink,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  userService = inject(UserService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  private realtime = inject(RealtimeService);

  loading = signal(true);
  error = signal<string | null>(null);
  dashboard = signal<DashboardResponse | null>(null);

  pipeLocale = 'en-US';
  Math: any;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }

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
          this.error.set(backendMsg || `Error loading dashboard (${err.status || 0})`);
          this.loading.set(false);
        }
      });
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

  get projectsForCard() {
    return (this.dashboard()?.projects ?? []).slice(0, 4);
  }

  projectBusinessUnit(project: ProjectCard): string {
    return String(project?.businessUnitName ?? project?.businessUnit ?? '').trim() || '-';
  }

  projectStatusLabel(status?: string | null): string {
    const normalized = String(status ?? '').trim().toUpperCase();
    if (!normalized) return '-';
    return normalized
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  toPct(value: number, total: number): number {
    if (!total) return 0;
    return (Math.max(0, value) / total) * 100;
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

    return 'User';
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

  deadlineMainLabel(days: number): string {
    return '';
  }

  deadlineDaysText(days: number): string {
    return `${Math.abs(days)} days`;
  }
}
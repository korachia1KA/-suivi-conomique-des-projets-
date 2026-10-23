import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, isPlatformBrowser, AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CostsService } from '../../services/costs/costs.service';
import { BusinessUnitService } from '../../services/businessUnit/business-unit';
import { AuthService } from '../../services/auth/auth.service';
import { CostLine } from '../../models/cost.model';
import { BusinessUnitOption } from '../../models/businessUnitOption.model';

@Component({
  selector: 'app-costs',
  standalone: true,
  imports: [
    AsyncPipe, DatePipe, DecimalPipe,
    FormsModule,
    RouterLink,
    TranslateModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './costs.component.html',
  styleUrl: './costs.component.scss',
})
export class CostsComponent implements OnInit {

  private costsService = inject(CostsService);
  private buService = inject(BusinessUnitService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  loading = signal(true);
  errorKey = signal<string | null>(null);
  rows = signal<CostLine[]>([]);

  q = signal('');
  buId = signal('');
  status = signal('');

  businessUnits = signal<BusinessUnitOption[]>([]);
  isAdmin = signal(false);

  pipeLocale = this.mapLocale(this.translate.getCurrentLang());

  readonly statusOptions = [
    { value: '',           labelKey: 'PROJECT.FILTER.ALL' },
    { value: 'PLANNING',   labelKey: 'PROJECT.STATUS.PLANNED' },
    { value: 'IN_PROGRESS',labelKey: 'PROJECT.STATUS.IN_PROGRESS' },
    { value: 'COMPLETED',  labelKey: 'PROJECT.STATUS.COMPLETED' },
  ];

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }

    this.isAdmin.set(this.authService.hasRole('ADMIN'));

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => (this.pipeLocale = this.mapLocale(e.lang)));

    if (this.isAdmin()) {
      this.buService.list()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(bus => this.businessUnits.set(bus));
    }

    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);

    this.costsService
      .list({
        q: this.q() || undefined,
        buId: this.buId() || undefined,
        status: this.status() || undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rows => { this.rows.set(rows ?? []); this.loading.set(false); },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          if (err.status === 401) this.authService.logout();
          const code = typeof err.error?.message === 'string' ? err.error.message : 'LOAD';
          this.errorKey.set(`COSTS.ERROR.${code}`);
        },
      });
  }

  onFilterChange(): void { this.load(); }

  resetFilters(): void {
    this.q.set('');
    this.buId.set('');
    this.status.set('');
    this.load();
  }

  totalRevenue = (): number => this.rows().reduce((s, r) => s + (r.estimatedRevenue ?? 0), 0);
  totalLabor   = (): number => this.rows().reduce((s, r) => s + (r.laborCost ?? 0), 0);
  totalExtra   = (): number => this.rows().reduce((s, r) => s + (r.extraCost ?? 0), 0);
  totalCost    = (): number => this.rows().reduce((s, r) => s + (r.totalCost ?? 0), 0);

  globalMargin = (): number | null => {
    const rev = this.totalRevenue();
    if (rev <= 0) return null;
    return ((rev - this.totalCost()) / rev) * 100;
  };

  marginClass(m: number | null): 'ok' | 'warn' | 'ko' | 'none' {
    if (m == null) return 'none';
    if (m < 15) return 'ko';
    if (m < 25) return 'warn';
    return 'ok';
  }

  private mapLocale(lang: string | undefined) {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }
}
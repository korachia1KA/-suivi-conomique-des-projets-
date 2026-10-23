import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ProjectsService } from '../../services/projects/projects';
import { RealtimeService } from '../../services/realtime/realtime';
import { AuthService } from '../../services/auth/auth.service';
import { ProjectDetail } from '../../models/projectDetail.model';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe,
    ReactiveFormsModule,
    TranslateModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule,
    MatDatepickerModule, MatNativeDateModule,
    MatProgressSpinnerModule, MatTooltipModule,
    RouterLink,
  ],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.scss',
})
export class ProjectDetailComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private projectsService = inject(ProjectsService);
  private realtime = inject(RealtimeService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  project = signal<ProjectDetail | null>(null);
  loading = signal(true);
  errorKey = signal<string | null>(null);
  savingExtra = signal(false);
  savingHours = signal(false);
  deletingHours = signal<string | null>(null);

  pipeLocale = this.mapLocale(this.translate.getCurrentLang());
  projectId = '';

  get p(): ProjectDetail {
    return this.project()!;
  }

  extraForm = this.fb.group({
    description: ['', [Validators.maxLength(255)]],
    cost: [null as number | null, [Validators.required, Validators.min(0.01)]],
    startDate: [null as Date | null],
    endDate: [null as Date | null],
  });

  addHoursForm = this.fb.group({
    month: [this.firstOfCurrentMonth(), [Validators.required]],
    hoursWorked: [null as number | null, [Validators.required, Validators.min(0)]],
    hourlyCost: [null as number | null],
    idUser: [null as string | null],
  });

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.projectId) {
      this.router.navigate(['/projects']);
      return;
    }

    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => (this.pipeLocale = this.mapLocale(e.lang)));

    this.load();

    this.realtime.projectCreated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evt => {
        if (evt.projectId === this.projectId) this.load();
      });

    this.realtime.projectCritical$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evt => {
        if (evt.projectId === this.projectId) this.load();
      });

    this.realtime.projectUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evt => {
        if (evt.projectId === this.projectId) this.load();
      });
  }

  private load(): void {
    this.loading.set(true);
    this.errorKey.set(null);

    this.projectsService.getDetail(this.projectId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (d) => { this.project.set(d); this.loading.set(false); },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          if (err.status === 401) this.authService.logout();
          const code = typeof err.error?.message === 'string' ? err.error.message : 'LOAD';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  submitExtraCost(): void {
    if (this.extraForm.invalid || this.savingExtra() || !this.project()) return;
    if (!this.authService.hasRole('ADMIN') && !this.authService.hasRole('MANAGER')) return;

    this.savingExtra.set(true);

    const v = this.extraForm.value;
    const payload = {
      description: (v.description ?? '').trim(),
      cost: Number(v.cost ?? 0),
      startDate: this.toIsoDate(v.startDate ?? null),
      endDate: this.toIsoDate(v.endDate ?? null),
    };

    this.projectsService.addExtraCost(this.projectId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingExtra.set(false);
          this.extraForm.reset({ description: '', cost: null, startDate: null, endDate: null });
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.savingExtra.set(false);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'CREATE';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  submitHours(): void {
    if (this.addHoursForm.invalid || this.savingHours() || !this.project()) return;

    this.savingHours.set(true);

    const v = this.addHoursForm.value;
    const payload = {
      idUser: v.idUser ?? null,
      date: this.toIsoDate(v.month ?? null)!,
      hoursWorked: Number(v.hoursWorked ?? 0),
      hourlyCost: v.hourlyCost != null ? Number(v.hourlyCost) : null,
    };

    this.projectsService.saveHours(this.projectId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingHours.set(false);
          this.addHoursForm.reset({
            month: this.firstOfCurrentMonth(),
            hoursWorked: null,
            hourlyCost: null,
            idUser: null,
          });
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.savingHours.set(false);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'CREATE';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  deleteHours(idUser: string, date: string): void {
    if (!this.authService.hasRole('ADMIN') && !this.authService.hasRole('MANAGER')) return;

    const key = `${idUser}_${date}`;
    if (this.deletingHours() === key) return;

    const confirmMsg = this.translate.instant('PROJECT.HOURS.CONFIRM_DELETE') as string;
    if (!confirm(confirmMsg)) return;

    this.deletingHours.set(key);

    this.projectsService.deleteHours(this.projectId, idUser, date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingHours.set(null);
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.deletingHours.set(null);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'DELETE';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  marginClass(m: number | null): 'ok' | 'warn' | 'ko' | 'none' {
    if (m == null) return 'none';
    if (m < 15) return 'ko';
    if (m < 25) return 'warn';
    return 'ok';
  }

  canManage(): boolean {
    return this.authService.hasRole('ADMIN') || this.authService.hasRole('MANAGER');
  }

  private firstOfCurrentMonth(): Date {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  private toIsoDate(d: Date | null): string | null {
    if (!d) return null;
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private mapLocale(lang: string | undefined) {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }
}
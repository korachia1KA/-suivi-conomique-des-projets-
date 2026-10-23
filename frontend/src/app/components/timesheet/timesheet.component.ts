import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
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

import { ProjectsService } from '../../services/projects/projects';
import { RealtimeService } from '../../services/realtime/realtime';
import { AuthService } from '../../services/auth/auth.service';
import { TimesheetEntry } from '../../models/timesheet.model';

@Component({
  selector: 'app-timesheet',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    TranslateModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './timesheet.component.html',
  styleUrl: './timesheet.component.scss',
})
export class TimesheetComponent implements OnInit {

  private projectsService = inject(ProjectsService);
  private realtime = inject(RealtimeService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  projectId = signal('');
  entries = signal<TimesheetEntry[]>([]);
  loading = signal(false);
  errorKey = signal<string | null>(null);
  saving = signal(false);
  deletingKey = signal<string | null>(null);
  isAdminOrManager = signal(false);

  pipeLocale = (this.translate.getCurrentLang() ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';

  form = this.fb.group({
    month: [this.firstOfCurrentMonth(), [Validators.required]],
    hoursWorked: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.isAdminOrManager.set(
      this.authService.hasRole('ADMIN') || this.authService.hasRole('MANAGER')
    );

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => {
        this.pipeLocale = (e.lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
      });

    this.realtime.projectUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evt => {
        if (evt.projectId === this.projectId()) this.load();
      });

    this.realtime.projectCritical$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(evt => {
        if (evt.projectId === this.projectId()) this.load();
      });
  }

  loadProject(idProject: string): void {
    const trimmed = (idProject ?? '').trim();
    if (!trimmed) return;
    if (this.projectId() === trimmed) return;
    this.projectId.set(trimmed);
    this.load();
  }

  load(): void {
    const id = this.projectId();
    if (!id) return;

    this.loading.set(true);
    this.errorKey.set(null);

    this.projectsService.listHours(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rows => { this.entries.set(rows ?? []); this.loading.set(false); },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'LOAD';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    const id = this.projectId();
    if (!id) return;

    this.saving.set(true);

    const v = this.form.value;
    const payload = {
      date: this.toIsoDate(v.month ?? null)!,
      hoursWorked: Number(v.hoursWorked ?? 0),
      hourlyCost: null as number | null,
    };

    this.projectsService.saveHours(id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.form.reset({ month: this.firstOfCurrentMonth(), hoursWorked: null });
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'SAVE';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
  }

  deleteEntry(idUser: string, date: string): void {
    if (!this.isAdminOrManager()) return;

    const key = `${idUser}_${date}`;
    if (this.deletingKey() === key) return;

    const confirmMsg = this.translate.instant('PROJECT.HOURS.CONFIRM_DELETE') as string;
    if (!confirm(confirmMsg)) return;

    const id = this.projectId();
    if (!id) return;

    this.deletingKey.set(key);

    this.projectsService.deleteHours(id, idUser, date)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingKey.set(null);
          this.load();
        },
        error: (err: HttpErrorResponse) => {
          this.deletingKey.set(null);
          const code = typeof err.error?.message === 'string' ? err.error.message : 'DELETE';
          this.errorKey.set(`PROJECT.ERROR.${code}`);
        },
      });
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
}
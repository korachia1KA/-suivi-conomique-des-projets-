import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl, FormArray, FormBuilder, FormControl, FormGroup,
  FormGroupDirective, NgForm, ReactiveFormsModule,
  ValidationErrors, ValidatorFn, Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { DateAdapter, ErrorStateMatcher, MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../services/auth/auth.service';
import { BusinessUnitService } from '../../../services/businessUnit/business-unit';
import { ProjectsService } from '../../../services/projects/projects';
import { UserService } from '../../../services/user/user';
import { UserProfile } from '../../../models/userProfile.model';
import { BusinessUnitOption } from '../../../models/businessUnitOption.model';
import { CreateProjectPayload } from '../../../models/project.model';

@Component({
  selector: 'app-project-create',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    RouterLink,
    TranslateModule,
    DecimalPipe,
  ],
  templateUrl: './project-create.html',
  styleUrls: ['./project-create.scss'],
})
export class ProjectCreate implements OnInit {
  private fb = inject(FormBuilder);
  private projectsService = inject(ProjectsService);
  private buService = inject(BusinessUnitService);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private translate = inject(TranslateService);
  private dateAdapter = inject(DateAdapter<Date>);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  saving = signal(false);
  errorKey = signal<string | null>(null);
  dirtyErrorStateMatcher = new DirtyErrorStateMatcher();

  isManager = signal(this.authService.hasRole('MANAGER'));
  isAdmin = signal(this.authService.hasRole('ADMIN'));

  businessUnits = signal<BusinessUnitOption[]>([]);

  private localeSig = signal(this.mapLocale(this.translate.getCurrentLang()));

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    businessUnitId: [''],
    startDate: [null as Date | null, Validators.required],
    plannedEndDate: [null as Date | null, [Validators.required, this.plannedEndDateValidator()]],
    estimatedRevenue: ['', [Validators.required, this.localizedNumberValidator({ min: 0 })]],
    estimatedCost: ['', [Validators.required, this.localizedNumberValidator({ min: 0 })]],
    team: this.fb.array<FormGroup>([]),
  });

  get team(): FormArray<FormGroup> {
    return this.form.controls.team;
  }

  readonly teamRoleOptions = [
    'Full-stack Developer',
    'Sistemista',
    'Cyber Security Analyst',
    'IT Project Manager',
    'Data Analyst',
  ] as const;

  assignableUsers = signal<any[]>([]);
  assignableUsersLoaded = signal(false);
  assignableUsersLoading = signal(false);

  assignableUserId(u: any): string {
    return String(u?.userId ?? u?.id ?? u?.id_user ?? u?.idUser ?? '').trim();
  }

  assignableUserLabel(u: any): string {
    return String(u?.fullName ?? u?.name ?? u?.displayName ?? u?.userName ?? u?.username ?? u?.email ?? '').trim();
  }

  assignableUserDisplay = (value: any): string => {
    const raw = String(value ?? '').trim();
    if (!raw) return '';
    const match = (this.assignableUsers() ?? []).find(u => this.assignableUserId(u) === raw);
    return match ? this.assignableUserLabel(match) : raw;
  };

  filteredAssignableUsers(query: unknown): any[] {
    const q = String(query ?? '').trim().toLowerCase();
    const list = this.assignableUsers() ?? [];
    if (!q) return list;
    if (list.some(u => this.assignableUserId(u).toLowerCase() === q)) return list;
    return list.filter(u => this.assignableUserLabel(u).toLowerCase().includes(q));
  }

  private assignableUserIdValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const v = String(control.value ?? '').trim();
      if (!v) return null;
      const list = this.assignableUsers() ?? [];
      const ok = list.some(u => this.assignableUserId(u) === v);
      return ok ? null : { invalidUser: true };
    };
  }

  private teamRowIsActive(group: AbstractControl | null | undefined): boolean {
    const v: any = group?.value;
    return !!String(v?.userId ?? '').trim()
      || !!String(v?.role ?? '').trim()
      || v?.days != null
      || v?.hourlyCost != null;
  }

  private requiredIfTeamRowActive(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      if (!parent) return null;
      if (!this.teamRowIsActive(parent)) return null;

      const v = control.value;
      if (v == null) return { required: true };
      if (typeof v === 'string' && !v.trim()) return { required: true };
      return null;
    };
  }

  private numberLocale(): string {
    return this.localeSig();
  }

  private localeSeparators(locale: string): { group: string; decimal: string } {
    const parts = new Intl.NumberFormat(locale).formatToParts(1000.1);
    const group = parts.find(p => p.type === 'group')?.value ?? ',';
    const decimal = parts.find(p => p.type === 'decimal')?.value ?? '.';
    return { group, decimal };
  }

  private parseLocalizedNumber(value: unknown): number | null {
    if (value == null) return null;
    if (typeof value === 'number') return Number.isFinite(value) ? value : null;

    const raw = String(value).trim();
    if (!raw) return null;

    const { decimal } = this.localeSeparators(this.numberLocale());

    let s = raw.replace(/\s+/g, '');
    s = s.replace(/[^\d,.\-]/g, '');
    s = s.replace(/(?!^)-/g, '');

    const hasDot = s.includes('.');
    const hasComma = s.includes(',');
    const dotCount = (s.match(/\./g) ?? []).length;
    const commaCount = (s.match(/,/g) ?? []).length;

    let decimalSep: '.' | ',' | null = null;

    if (hasDot && hasComma) {
      decimalSep = s.lastIndexOf('.') > s.lastIndexOf(',') ? '.' : ',';
    } else if (hasDot) {
      if (decimal === '.') {
        decimalSep = '.';
      } else if (dotCount === 1) {
        const pos = s.lastIndexOf('.');
        const fracLen = Math.max(0, s.length - pos - 1);
        if (fracLen <= 2) decimalSep = '.';
      }
    } else if (hasComma) {
      if (decimal === ',') {
        decimalSep = ',';
      } else if (commaCount === 1) {
        const pos = s.lastIndexOf(',');
        const fracLen = Math.max(0, s.length - pos - 1);
        if (fracLen <= 2) decimalSep = ',';
      }
    }

    let normalized = s;

    if (decimalSep) {
      const groupSep = decimalSep === '.' ? ',' : '.';
      normalized = normalized.split(groupSep).join('');
      const last = normalized.lastIndexOf(decimalSep);
      if (last >= 0) {
        const intPart = normalized.slice(0, last).split(decimalSep).join('');
        const fracPart = normalized.slice(last + 1).split(decimalSep).join('');
        normalized = `${intPart}.${fracPart}`;
      }
    } else {
      normalized = normalized.replace(/[.,]/g, '');
    }

    const n = Number(normalized);
    return Number.isFinite(n) ? n : null;
  }

  private formatLocalizedNumber(n: number, fractionDigits = 2): string {
    return new Intl.NumberFormat(this.numberLocale(), {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    }).format(n);
  }

  private localizedNumberValidator(opts: { min?: number } = {}): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value === '' || control.value == null) return null;
      const n = this.parseLocalizedNumber(control.value);
      if (n == null) return { invalidNumber: true };
      if (opts.min != null && n < opts.min) return { min: { min: opts.min, actual: n } };
      return null;
    };
  }

  formatMoneyControl(controlName: 'estimatedRevenue' | 'estimatedCost'): void {
    const ctrl = this.form.controls[controlName] as any;
    const raw = ctrl?.value;
    if (raw == null || String(raw).trim() === '') return;

    const n = this.parseLocalizedNumber(raw);
    if (n == null) return;

    ctrl.setValue(this.formatLocalizedNumber(n, 2), { emitEvent: false });
    ctrl.updateValueAndValidity({ emitEvent: false });
  }

  ngOnInit(): void {
    this.dateAdapter.setLocale(this.localeSig());

    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((e: any) => {
        const locale = this.mapLocale(e?.lang);
        this.localeSig.set(locale);
        this.dateAdapter.setLocale(locale);
        this.formatMoneyControl('estimatedRevenue');
        this.formatMoneyControl('estimatedCost');
      });

    if (!this.isManager()) {
      this.loadBusinessUnits();
    }

    if (this.isManager()) {
      this.form.controls.businessUnitId.clearValidators();
      this.form.controls.businessUnitId.updateValueAndValidity({ emitEvent: false });
      this.form.controls.businessUnitId.disable({ emitEvent: false });
    } else {
      this.form.controls.businessUnitId.setValidators([Validators.required]);
      this.form.controls.businessUnitId.updateValueAndValidity({ emitEvent: false });
      this.form.controls.businessUnitId.enable({ emitEvent: false });
    }

    this.userService.getMe()
      .pipe(
        catchError(() => of(null as UserProfile | null)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(profile => {
        const role = String((profile as any)?.roleName ?? (profile as any)?.role?.name ?? '').toUpperCase();
        const manager = this.authService.hasRole('MANAGER') || role.includes('MANAGER');
        const admin = this.authService.hasRole('ADMIN') || role.includes('ADMIN');

        this.isManager.set(manager);
        this.isAdmin.set(admin);

        if (manager) {
          this.form.controls.businessUnitId.clearValidators();
          this.form.controls.businessUnitId.updateValueAndValidity({ emitEvent: false });
          this.form.controls.businessUnitId.disable({ emitEvent: false });
        } else {
          this.form.controls.businessUnitId.setValidators([Validators.required]);
          this.form.controls.businessUnitId.updateValueAndValidity({ emitEvent: false });
          this.form.controls.businessUnitId.enable({ emitEvent: false });

          if (this.businessUnits().length === 0) {
            this.loadBusinessUnits();
          }
        }
      });

    this.form.controls.startDate.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.form.controls.plannedEndDate.updateValueAndValidity({ emitEvent: false }));

    this.form.controls.plannedEndDate.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.form.controls.plannedEndDate.updateValueAndValidity({ emitEvent: false }));

    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => { if (this.errorKey()) this.errorKey.set(null); });
  }

  private loadBusinessUnits(): void {
    this.buService.list()
      .pipe(
        catchError(() => of([] as BusinessUnitOption[])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(bus => this.businessUnits.set(bus));
  }

  private loadAssignableUsersOnce(): void {
    if (this.assignableUsersLoaded() || this.assignableUsersLoading()) return;

    this.assignableUsersLoading.set(true);
    this.projectsService
      .getAssignableUsers()
      .pipe(
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((list) => {
        this.assignableUsers.set(Array.isArray(list) ? list : []);
        this.assignableUsersLoaded.set(true);
        this.assignableUsersLoading.set(false);
      });
  }

  private mapLocale(lang: string | undefined): string {
    return (lang ?? 'en').startsWith('it') ? 'it-IT' : 'en-US';
  }

  private plannedEndDateValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const end = control.value as Date | null;
      if (!end) return null;

      const parent = control.parent as FormGroup | null;
      const start = parent?.get('startDate')?.value as Date | null;

      const endDay = this.startOfDayMs(end.getTime());
      const todayDay = this.startOfDayMs(Date.now());

      if (endDay < todayDay) return { endBeforeToday: true };

      if (start instanceof Date) {
        const startDay = this.startOfDayMs(start.getTime());
        if (endDay < startDay) return { endBeforeStart: true };
      }

      return null;
    };
  }

  private startOfDayMs(ms: number): number {
    const d = new Date(ms);
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  }

  submit(): void {
    if (this.saving()) return;
    this.errorKey.set(null);

    const raw = this.form.getRawValue();
    const manager = this.isManager();

    for (const row of this.team.controls) {
      row.updateValueAndValidity({ emitEvent: false });
      for (const key of Object.keys(row.controls ?? {})) {
        (row.controls as any)[key]?.updateValueAndValidity?.({ emitEvent: false });
      }
    }

    const activeTeamRows = this.team.controls.filter(c => this.teamRowIsActive(c));
    if (activeTeamRows.some(c => c.invalid)) {
      for (const row of activeTeamRows) row.markAllAsTouched();
      return;
    }

    if (!manager) {
      const bu = String(raw.businessUnitId ?? '').trim();
      if (!bu) {
        this.form.controls.businessUnitId.markAsTouched();
        return;
      }
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const estimatedRevenue = this.parseLocalizedNumber(raw.estimatedRevenue);
    const estimatedCost = this.parseLocalizedNumber(raw.estimatedCost);
    if (estimatedRevenue == null || estimatedCost == null) {
      this.form.controls.estimatedRevenue.markAsTouched();
      this.form.controls.estimatedCost.markAsTouched();
      return;
    }

    const payload: CreateProjectPayload = {
      name: String(raw.name ?? '').trim(),
      startDate: this.toIsoDate(raw.startDate!),
      plannedEndDate: this.toIsoDate(raw.plannedEndDate!),
      estimatedRevenue,
      estimatedCost,
      ...(manager ? {} : { businessUnitId: String(raw.businessUnitId ?? '').trim() }),
      ...(activeTeamRows.length
        ? {
            users: activeTeamRows.map(ctrl => {
              const v: any = ctrl.value;
              return {
                userId: String(v?.userId ?? '').trim(),
                hourlyCost: Number(v?.hourlyCost ?? 0),
              };
            }),
          }
        : {}),
    };

    this.saving.set(true);

    this.projectsService
      .create(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.router.navigate(['/projects']);
        },
        error: (err) => {
          console.error('Project create error', err, payload);
          this.saving.set(false);
          const backendMsg = typeof (err as any)?.error?.message === 'string' ? (err as any).error.message : null;

          if (backendMsg === 'USER_REQUIRED') {
            this.errorKey.set('PROJECT.ERROR.USER_REQUIRED');
            return;
          }
          if (backendMsg === 'MANAGER_HAS_NO_BU') {
            this.errorKey.set('PROJECT.ERROR.MANAGER_HAS_NO_BU');
            return;
          }
          if (backendMsg?.startsWith('USER_HAS_NO_BU')) {
            this.errorKey.set('PROJECT.ERROR.USER_HAS_NO_BU');
            return;
          }
          if (backendMsg?.startsWith('BUSINESS_UNIT_NOT_FOUND')) {
            this.errorKey.set('PROJECT.ERROR.BUSINESS_UNIT_NOT_FOUND');
            return;
          }
          if (backendMsg === 'BUSINESS_UNIT_REQUIRED') {
            this.errorKey.set('PROJECT.ERROR.BUSINESS_UNIT_REQUIRED');
            return;
          }
          if (backendMsg === 'USER_BUSINESS_UNIT_MISMATCH') {
            this.errorKey.set('PROJECT.ERROR.USER_BUSINESS_UNIT_MISMATCH');
            return;
          }
          this.errorKey.set('PROJECT.ERROR.CREATE');
        },
      });
  }

  private toIsoDate(d: Date): string {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  addTeamRow(): void {
    this.loadAssignableUsersOnce();

    const requiredIfActive = this.requiredIfTeamRowActive();
    const validUserId = this.assignableUserIdValidator();
    this.team.push(
      this.fb.group({
        userId: ['', [requiredIfActive, validUserId]],
        role: [null as string | null, [requiredIfActive]],
        days: [null as number | null, [requiredIfActive, Validators.min(0)]],
        hourlyCost: [null as number | null, [requiredIfActive, Validators.min(0)]],
      })
    );
  }

  removeTeamRow(index: number): void {
    this.team.removeAt(index);
  }

  expectedMarginAmount(): number {
    const revenue = this.parseLocalizedNumber(this.form.controls.estimatedRevenue.value) ?? 0;
    const cost = this.parseLocalizedNumber(this.form.controls.estimatedCost.value) ?? 0;
    return revenue - cost;
  }

  expectedMarginPct(): number | null {
    const revenue = this.parseLocalizedNumber(this.form.controls.estimatedRevenue.value) ?? 0;
    if (!revenue) return null;
    const cost = this.parseLocalizedNumber(this.form.controls.estimatedCost.value) ?? 0;
    return ((revenue - cost) / revenue) * 100;
  }

  get expectedMarginNegative(): boolean {
    return this.expectedMarginAmount() < 0;
  }
}

class DirtyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const submitted = !!form?.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || submitted));
  }
}
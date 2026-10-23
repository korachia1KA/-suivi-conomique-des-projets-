import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { UserService } from '../../services/user/user';
import { UserProfile } from '../../models/userProfile.model';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { DateAdapter, MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiErrorMapperService } from '../../services/core/api-error-mapper';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatListModule
  ],
  templateUrl: './user.html',
  styleUrl: './user.scss',
})
export class UserComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private translate = inject(TranslateService);
  private errorMapper = inject(ApiErrorMapperService);
  private dateAdapter = inject(DateAdapter<Date>);
  private destroyRef = inject(DestroyRef);

  loading = signal(true);
  globalErrorKey = signal<string | null>(null);
  passwordErrorKey = signal<string | null>(null); 
  sectionErrorKey = signal<string | null>(null);

  passwordEdit = signal(false);
  loadingPassword = signal(false);

  hideCurrent = true;
  hideNew = true;
  hideConfirm = true;

  editPersonal = signal(false);
  editResidence = signal(false);
  editContacts = signal(false);
  editBank = signal(false);

  saving = signal(false);

  get currentLang() {
    return this.translate.getCurrentLang() || 'it';
  }

  setLang(lang: string) {
    this.translate.use(lang);
    if (typeof window !== 'undefined') localStorage.setItem('lang', lang);
  }

  profileForm = this.fb.group({
    firstName: [{ value: '', disabled: true }],
    lastName: [{ value: '', disabled: true }],
    fiscalCode: [{ value: '', disabled: true }],
    idCardNumber: [{ value: '', disabled: true }],
    birthDay: [{ value: '', disabled: true }], 
    birthPlace: [{ value: '', disabled: true }],
    address: [{ value: '', disabled: true }],
    city: [{ value: '', disabled: true }],
    phoneNumber: [{ value: '', disabled: true }],
    iban: [{ value: '', disabled: true }],
    ibanHolder: [{ value: '', disabled: true }],
    email: [{ value: '', disabled: true }],
  });

  passwordForm = this.fb.group(
    {
      currentPassword: this.fb.nonNullable.control('', [Validators.required]),
      newPassword: this.fb.nonNullable.control('', [
        Validators.required,
        Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{8,}$/),
      ]),
      confirmNewPassword: this.fb.nonNullable.control('', [Validators.required]),
    },
    { validators: this.passwordMatchValidator }
  );

  constructor() {
    this.passwordForm.disable();
  }


  ngOnInit(): void {
    this.dateAdapter.setLocale(this.dateLocale);
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.dateAdapter.setLocale(this.dateLocale));

    this.loadProfile();
  }

  private loadProfile() {
    this.loading.set(true);
    this.globalErrorKey.set(null);

    this.userService.getMe().subscribe({
      next: (profile: UserProfile) => {
       
        const birth = profile.birthDay ? profile.birthDay.slice(0, 10) : '';

        this.profileForm.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          fiscalCode: profile.fiscalCode ?? '',
          idCardNumber: profile.idCardNumber ?? '',
          birthDay: birth,
          birthPlace: profile.birthPlace ?? '',
          address: profile.address ?? '',
          city: profile.city ?? '',
          phoneNumber: profile.phoneNumber ?? '',
          iban: profile.iban ?? '',
          ibanHolder: profile.ibanHolder ?? '',
          email: profile.email ?? '',
        });

        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        console.error(err);
        this.loading.set(false);
        const backendCode = this.errorMapper.map(err);
  
        this.passwordErrorKey.set(`USER.ERROR.${backendCode}`);
            },
    });
  }

  enablePasswordEdit() {
    this.passwordEdit.set(true);
    this.passwordErrorKey.set(null);
    this.passwordForm.reset();
    this.passwordForm.enable();
  }

  cancelPasswordEdit() {
    this.passwordEdit.set(false);
    this.passwordErrorKey.set(null);
    this.passwordForm.reset();
    this.passwordForm.disable();
    this.hideCurrent = true;
    this.hideNew = true;
    this.hideConfirm = true;
  }

  onChangePassword() {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched(); 
      return;
    }

    this.loadingPassword.set(true);
    this.passwordErrorKey.set(null);

    const payload = this.passwordForm.getRawValue();

    this.userService.updatePassword(payload).subscribe({
      next: () => {
        this.loadingPassword.set(false);
        this.cancelPasswordEdit();
      },
      error: (err: HttpErrorResponse) => {
        this.loadingPassword.set(false);
        const backendCode = this.errorMapper.map(err);

        this.passwordErrorKey.set(`USER.ERROR.${backendCode}`);
      }
    });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const p = control.get('newPassword')?.value;
    const c = control.get('confirmNewPassword')?.value;
    return p && c && p !== c ? { passwordMismatch: true } : null;
  }

  enableSection(section: 'personal'|'residence'|'contacts'|'bank') {
  this.sectionErrorKey.set(null);

  if (section === 'personal') {
    this.editPersonal.set(true);
    this.profileForm.get('firstName')?.enable();
    this.profileForm.get('lastName')?.enable();
    this.profileForm.get('fiscalCode')?.enable();
    this.profileForm.get('idCardNumber')?.enable();
    this.profileForm.get('birthDay')?.enable();
    this.profileForm.get('birthPlace')?.enable();
  }

  if (section === 'residence') {
    this.editResidence.set(true);
    this.profileForm.get('address')?.enable();
    this.profileForm.get('city')?.enable();
  }

  if (section === 'contacts') {
    this.editContacts.set(true);
    this.profileForm.get('phoneNumber')?.enable();
  }

  if (section === 'bank') {
    this.editBank.set(true);
    this.profileForm.get('iban')?.enable();
    this.profileForm.get('ibanHolder')?.enable();
  }
}

cancelSection(section: 'personal'|'residence'|'contacts'|'bank') {
  this.sectionErrorKey.set(null);

  this.loadProfile();

  if (section === 'personal') {
    this.editPersonal.set(false);
    this.profileForm.get('firstName')?.disable();
    this.profileForm.get('lastName')?.disable();
    this.profileForm.get('fiscalCode')?.disable();
    this.profileForm.get('idCardNumber')?.disable();
    this.profileForm.get('birthDay')?.disable();
    this.profileForm.get('birthPlace')?.disable();
  }
  if (section === 'residence') {
    this.editResidence.set(false);
    this.profileForm.get('address')?.disable();
    this.profileForm.get('city')?.disable();
  }
  if (section === 'contacts') {
    this.editContacts.set(false);
    this.profileForm.get('phoneNumber')?.disable();
    this.profileForm.get('email')?.disable();
  }
  if (section === 'bank') {
    this.profileForm.get('iban')?.disable();
    this.profileForm.get('ibanHolder')?.disable();
    this.editBank.set(false);
  }
}


  saveSection(section: 'personal'|'residence'|'contacts'|'bank') {
    this.saving.set(true);
    this.sectionErrorKey.set(null);

    const v = this.profileForm.getRawValue();

    if (section === 'personal') {
      const payload = {
        firstName: v.firstName ?? '',
        lastName: v.lastName ?? '',
        fiscalCode: v.fiscalCode ?? '',
        idCardNumber: v.idCardNumber ?? '',
        birthDay: v.birthDay ?? '',
        birthPlace: v.birthPlace ?? '',
      };

      this.userService.updatePersonal(payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.userService.updateLocalName(payload.firstName);
          this.editPersonal.set(false);
          this.profileForm.get('firstName')?.disable();
          this.profileForm.get('lastName')?.disable();
          this.profileForm.get('fiscalCode')?.disable();
          this.profileForm.get('idCardNumber')?.disable();
          this.profileForm.get('birthDay')?.disable();
          this.profileForm.get('birthPlace')?.disable();
          this.loadProfile();
        },
        error: () => {
          this.saving.set(false);
          this.sectionErrorKey.set('Errore nel salvataggio anagrafica');
        }
      });
      return;
    }

    if (section === 'residence') {
      const payload = {
        address: v.address ?? '',
        city: v.city ?? '',
      };

      this.userService.updateResidence(payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.editResidence.set(false);
          this.profileForm.get('address')?.disable();
          this.profileForm.get('city')?.disable();
          this.loadProfile();
        },
        error: () => {
          this.saving.set(false);
          this.sectionErrorKey.set('Errore nel salvataggio residenza');
        }
      });
      return;
    }

    if (section === 'contacts') {
      const payload = { 
        phoneNumber: v.phoneNumber ?? '',
        email: v.email ?? ''};

      this.userService.updateContacts(payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.editContacts.set(false);
          this.profileForm.get('phoneNumber')?.disable();
          this.profileForm.get('email')?.disable();
          this.loadProfile();
        },
        error: () => {
          this.saving.set(false);
          this.sectionErrorKey.set('Errore nel salvataggio recapiti');
        }
      });
      return;
    }

    if (section === 'bank') {
      const payload = {
        iban: v.iban ?? '',
        ibanHolder: v.ibanHolder ?? '',
      };

      this.userService.updateBank(payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.editBank.set(false);
          this.profileForm.get('iban')?.disable();
          this.profileForm.get('ibanHolder')?.disable();
          this.loadProfile();
        },
        error: () => {
          this.saving.set(false);
          this.sectionErrorKey.set('Errore nel salvataggio dati bancari');
        }
      });
      return;
    }
  }

  get dateLocale(): string {
    return this.currentLang.startsWith('it') ? 'it-IT' : 'en-US';
  }

  get birthDayValue(): Date | null {
    return this.parseLocalDate(this.profileForm.controls.birthDay.value ?? '');
  }

  onBirthDayChange(value: Date | null): void {
    this.profileForm.controls.birthDay.setValue(this.formatLocalDate(value));
  }

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
}
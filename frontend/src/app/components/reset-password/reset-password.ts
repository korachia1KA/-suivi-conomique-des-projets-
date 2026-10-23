import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, ValidationErrors, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../services/auth/auth.service';
import { ApiErrorMapperService } from '../../services/core/api-error-mapper';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    TranslateModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss'
})
export class ResetPasswordComponent {

  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private platformId = inject(PLATFORM_ID);
  private errorMapper = inject(ApiErrorMapperService);

  loading = false;
  done = signal(false);
  errorMessage = signal<string | null>(null);

  hidePassword = true;

  email = signal<string>('');
  step = signal<'otp' | 'password'>('otp');   
  resetToken = signal<string | null>(null);   

  otpForm = this.fb.group({
    otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
  });

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{8,}$/)]],
    confirmNewPassword: ['', [Validators.required]],
  }, { validators: this.passwordMatchValidator });

  constructor() {
    const memEmail = this.auth.resetEmailSig();

    if (!memEmail) {
     this.router.navigate(['/auth/forgot-password']);
      return;
  }

  this.email.set(memEmail);
  }

  get currentLang() {
    return this.translate.getCurrentLang() || 'it';
  }

  setLang(lang: string) {
    this.translate.use(lang);
    if (isPlatformBrowser(this.platformId)) localStorage.setItem('lang', lang);
  }

  onSubmitOtp() {
    if (this.otpForm.invalid || !this.email()) return;

    this.loading = true;
    this.errorMessage.set(null);

    const otp = this.otpForm.value.otp!.trim();

    this.auth.verifyOtp(this.email(), otp).subscribe({
      next: (res: { resetToken: string }) => {
        this.loading = false;
        this.resetToken.set(res.resetToken);
        this.step.set('password');
        this.otpForm.reset();
      },
      error: (err) => {
        this.loading = false;
        const backendCode = err?.error?.message || 'GENERIC';
        this.translate.get(`RESET.ERROR.${backendCode}`).subscribe(msg => {
          this.errorMessage.set(msg);
          this.auth.resetEmailSig.set(null);
        });
      }
    });
  }

  onSubmitPassword() {
    if (this.passwordForm.invalid || !this.resetToken()) return;

    this.loading = true;
    this.errorMessage.set(null);

    const payload = {
      resetToken: this.resetToken()!,
      newPassword: this.passwordForm.value.newPassword!,
      confirmNewPassword: this.passwordForm.value.confirmNewPassword!,
    };

    this.auth.resetPassword(payload).subscribe({
      next: () => {
        this.loading = false;
        this.done.set(true);
        this.resetToken.set(null);
      },
      error: (err) => {
        this.loading = false;
        const backendCode = this.errorMapper.map(err);
        this.translate.get(`RESET.ERROR.${backendCode}`).subscribe(msg => {
          this.errorMessage.set(msg);
        });
      }
    });
  }

  backToLogin() {
    this.step.set('otp');
    this.resetToken.set(null);
    this.passwordForm.reset();
    this.errorMessage.set(null);
    this.auth.resetEmailSig.set(null);
    this.router.navigate(['/auth/login']);
  }
  
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmNewPassword');

    if (!password || !confirmPassword) return null;

    if (password.value !== confirmPassword.value) {
   
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
  
      if (confirmPassword.hasError('passwordMismatch')) {
        confirmPassword.setErrors(null);
      }
      return null;
    }
  }
}
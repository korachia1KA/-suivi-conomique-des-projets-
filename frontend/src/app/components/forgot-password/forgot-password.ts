import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
  selector: 'app-forgot-password',
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
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss'
})

export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private platformId = inject(PLATFORM_ID);
  private errorMapper = inject(ApiErrorMapperService);

  loading = false;
  success = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  get email() { return this.form.get('email'); }

  get currentLang() {
    return this.translate.getCurrentLang() || 'it';
  }

  setLang(lang: string) {
    this.translate.use(lang);
    if (isPlatformBrowser(this.platformId)) localStorage.setItem('lang', lang);
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage.set(null);

    const email = this.form.value.email!.trim();

    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.loading = false;
        this.success.set(true);
        this.auth.setResetEmail(email);    
        this.router.navigate(['/auth/reset-password']);
      },
      error: (err) => {
        this.loading = false;
        const backendCode = this.errorMapper.map(err);
        this.translate.get(`FORGOT.ERROR.${backendCode}`).subscribe(msg => {
          this.errorMessage.set(msg);
        });
      }
    });
  }
}
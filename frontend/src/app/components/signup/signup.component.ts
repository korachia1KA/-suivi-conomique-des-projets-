import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../services/auth/auth.service';
import { Signup } from '../../models/signup.model';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterModule } from '@angular/router';
import { ApiErrorMapperService } from '../../services/core/api-error-mapper';
import { MatProgressSpinner } from "@angular/material/progress-spinner";

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatCardModule,
    TranslateModule,
    MatIconModule,
    RouterModule,
    RouterLink,
    MatProgressSpinner
],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private errorMapper = inject(ApiErrorMapperService);

  isRegistered = signal(false); 
  errorMessage = signal<string | null>(null);
  hidePassword = true;
  loading = signal(false);

  get currentLang() {
    return this.translate.getCurrentLang() || 'it';
  }
  
  setLang(lang: string) {
    this.translate.use(lang);
    if (typeof window !== 'undefined') {
      localStorage.setItem('lang', lang);
    }
  }

  signupForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{8,}$/)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

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
  onSubmit() {
    if (this.signupForm.invalid || this.loading()) return;

    const { confirmPassword, ...signupData } = this.signupForm.value;

    this.errorMessage.set(null);
    this.loading.set(true);

    this.authService.signup(signupData as Signup).subscribe({
      next: (res) => {
        console.log('Registrazione completata!', res);
        this.isRegistered.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);

        const backendCode = this.errorMapper.map(err);
        const i18nKey = `SIGNUP.ERROR.${backendCode}`;

        this.translate.get(i18nKey).subscribe((msg) => {
          this.errorMessage.set(msg);
        });
      }
    });
  }
}
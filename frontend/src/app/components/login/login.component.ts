import { CommonModule } from '@angular/common';
import { Component, inject, signal} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardContent, MatCard } from "@angular/material/card";
import { MatFormField, MatLabel, MatError, MatInputModule } from "@angular/material/input";
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth/auth.service';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ApiErrorMapperService } from '../../services/core/api-error-mapper';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    MatCardContent,
    MatCard,
    MatFormField,
    MatLabel,
    MatCheckboxModule,
    TranslateModule, 
    MatError,
    MatIconModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatButtonModule,
    RouterLink,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})

export class Login {

  private router = inject(Router);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private errorMapper = inject(ApiErrorMapperService);

  get currentLang() {
    return this.translate.getCurrentLang() || 'it';
  }

  setLang(lang: string) {
    this.translate.use(lang);
    if (typeof window !== 'undefined') {
      localStorage.setItem('lang', lang);
    }
  }

  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [false]
  });

  hidePassword = true;
  loading = false;
  errorMessage = signal<string | null>(null);


  onLogin() {
    if (this.loginForm.valid) {
      this.loading = true;
      this.errorMessage.set(null);

      this.authService.login(this.loginForm.value).subscribe({
        next: (res) => {
          this.loading = false;

          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
      
          const backendCode = this.errorMapper.map(err);
          const i18nKey = `LOGIN.ERROR.${backendCode}`;
          
          this.translate.get(i18nKey).subscribe(msg => {
            this.errorMessage.set(msg);
          });
        }
      });
    }
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
  
}
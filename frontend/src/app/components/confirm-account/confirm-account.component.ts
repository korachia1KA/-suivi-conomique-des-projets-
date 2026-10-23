import { Component, inject, signal, afterNextRender } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { MatProgressSpinner, MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatCardContent, MatCard } from "@angular/material/card";
import { MatError, MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';

type ConfirmStatus = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-confirm-account',
  imports: [TranslateModule,
    MatIcon,
    CommonModule,
    MatCardContent,
    MatCard,
    TranslateModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatButtonModule,
    RouterLink,
    MatIconModule, MatCardContent, MatCard],
  standalone: true,
  templateUrl: './confirm-account.component.html',
  styleUrls: ['./confirm-account.component.scss'],

})
export class ConfirmAccountComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  
  status = signal<ConfirmStatus>('loading');
  errorMessage = signal<string>('');
  userEmail = signal<string>('');

  constructor() {
    afterNextRender(() => {
      this.initConfirmation();
    });
  }

  private initConfirmation(): void {
    
    if (sessionStorage.getItem('account_confirmed') === 'true') {
      this.status.set('success');
      return;
    }

    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      this.hideTokenInAddressBar();
      this.confirmAccount(token);
    } else {
      this.status.set('error');
      this.errorMessage.set('CONFIRM_ACCOUNT.ERROR.MISSING_TOKEN');
    }
  }

  private hideTokenInAddressBar(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true
    });
  }

  private confirmAccount(token: string): void {
    this.status.set('loading');
    this.authService.confirm(token).subscribe({
      next: (data: any) => {
        this.userEmail.set(data.email); 
        this.status.set('success');
        sessionStorage.setItem('account_confirmed', 'true');
      },
      error: (err) => {
        this.status.set('error');
        if(err.status === 0){
          this.errorMessage.set('CONFIRM_ACCOUNT.ERROR.SERVER_OFFLINE');
        }else{
          const backendCode = err.error?.message;
          const i18nKey = `CONFIRM_ACCOUNT.ERROR.${backendCode}`;

          this.translate.get(i18nKey).subscribe((msg) => {
            this.errorMessage.set(msg);
          });
        }
      }
    });
  }
}
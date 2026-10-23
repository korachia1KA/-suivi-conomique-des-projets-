import { Component, DestroyRef, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AdminUserService } from '../../../services/admin/admin-user.service';
import { BusinessUnitService } from '../../../services/businessUnit/business-unit';
import { AuthService } from '../../../services/auth/auth.service';
import { AdminUser } from '../../../models/adminUser.model';
import { BusinessUnitOption } from '../../../models/businessUnitOption.model';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TranslateModule,
    MatCardModule, MatFormFieldModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {

  private adminUserService = inject(AdminUserService);
  private buService = inject(BusinessUnitService);
  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  loading = signal(true);
  saving = signal<string | null>(null);   // idUser being saved
  errorKey = signal<string | null>(null);

  users = signal<AdminUser[]>([]);
  businessUnits = signal<BusinessUnitOption[]>([]);

  /** draft BU per user, keyed by idUser */
  drafts: Record<string, string | null> = {};

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) { this.loading.set(false); return; }

    if (!this.authService.hasRole('ADMIN')) {
      this.errorKey.set('ADMIN.USERS.ERROR.NOT_ADMIN');
      this.loading.set(false);
      return;
    }

    this.buService.list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(bus => this.businessUnits.set(bus));

    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.errorKey.set(null);

    this.adminUserService.list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: users => {
          this.users.set(users ?? []);
          this.drafts = {};
          for (const u of users) {
            this.drafts[u.idUser] = u.businessUnitId;
          }
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorKey.set(
            err.status === 403 ? 'ADMIN.USERS.ERROR.NOT_ADMIN' : 'ADMIN.USERS.ERROR.LOAD'
          );
        },
      });
  }

  isChanged(u: AdminUser): boolean {
    return (this.drafts[u.idUser] ?? null) !== (u.businessUnitId ?? null);
  }

  onSelect(u: AdminUser, value: string): void {
    this.drafts[u.idUser] = value === '' ? null : value;
  }

  save(u: AdminUser): void {
    if (!this.isChanged(u) || this.saving()) return;

    this.saving.set(u.idUser);
    this.adminUserService.assignBusinessUnit(u.idUser, this.drafts[u.idUser] ?? null)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(null);
          this.load();
        },
        error: () => {
          this.saving.set(null);
          this.errorKey.set('ADMIN.USERS.ERROR.SAVE');
        },
      });
  }

  displayName(u: AdminUser): string {
    const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
    return name || u.email;
  }
}
import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth/auth.service';
import { RouterModule } from '@angular/router';
import { UserService } from '../../services/user/user';
import { RealtimeService } from '../../services/realtime/realtime';
import { NotificationService } from '../../services/notifications/notification.service';
import { NotificationPanelComponent } from '../notification-panel/notification-panel.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
    MatToolbarModule,
    TranslateModule,
    RouterModule,
    NotificationPanelComponent,
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  authService = inject(AuthService);
  userService = inject(UserService);
  notificationService = inject(NotificationService);
  private realtime = inject(RealtimeService);
  private destroyRef = inject(DestroyRef);

  projectCreatedBellOn  = signal(false);   // green pulse
  projectCriticalBellOn = signal(false);   // red pulse

  private createdTimer:  ReturnType<typeof setTimeout> | null = null;
  private criticalTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) return;

    // Initial load of the notification count
    this.notificationService.list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: () => { /* ignore */ } });

    // Load profile + subscribe to WS topics depending on the role
    this.userService.getMe()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(profile => {
        this.userService.currentUser.set(profile);
        const buId = String(profile?.businessUnitId ?? profile?.businessUnit?.idBusinessUnit ?? '').trim();

        if (this.authService.hasRole('ADMIN')) {
          this.realtime.watchAllProjectsCreated();
          this.realtime.watchAllProjectsCritical();
          this.realtime.watchAllProjectsUpdated();
        } else if (this.authService.hasRole('MANAGER') && buId) {
          this.realtime.watchBuProjectsCreated(buId);
          this.realtime.watchBuProjectsCritical(buId);
          this.realtime.watchBuProjectsUpdated(buId);
        }
      });

    // Live notifications → bell pulse
    this.realtime.notification$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(n => {
        if (n.kind === 'PROJECT_CRITICAL') this.flashCritical();
        else                               this.flashCreated();
      });

    // Also react on the raw broadcast events (in case the notification queue is down)
    this.realtime.projectCreated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.flashCreated());

    this.realtime.projectCritical$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.flashCritical());

    // projectUpdated does NOT flash the bell — it's just a data refresh signal.
    // (If you want a flash, uncomment the lines below.)
    // this.realtime.projectUpdated$
    //   .pipe(takeUntilDestroyed(this.destroyRef))
    //   .subscribe(() => this.flashCreated());

    this.destroyRef.onDestroy(() => {
      if (this.createdTimer)  clearTimeout(this.createdTimer);
      if (this.criticalTimer) clearTimeout(this.criticalTimer);
      // ⚠️ Do NOT call realtime.disconnect() here:
      // the STOMP client is a singleton and must only be closed on logout.
    });
  }

  onLogout(): void {
    this.realtime.disconnect();
    this.authService.logout();
  }

  private flashCreated(): void {
    this.projectCreatedBellOn.set(true);
    if (this.createdTimer) clearTimeout(this.createdTimer);
    this.createdTimer = setTimeout(() => this.projectCreatedBellOn.set(false), 3500);
  }

  private flashCritical(): void {
    this.projectCriticalBellOn.set(true);
    if (this.criticalTimer) clearTimeout(this.criticalTimer);
    this.criticalTimer = setTimeout(() => this.projectCriticalBellOn.set(false), 5000);
  }

  userInitial = computed(() => {
    const user = this.userService.currentUser();
    return user?.firstName ? user.firstName[0].toUpperCase() : 'U';
  });

  canSeeProjects = computed(() => {
    const roles = this.authService.getRoles();
    if (roles.length > 0) {
      const upper = roles.map(r => String(r).toUpperCase());
      return upper.some(r => r.includes('ADMIN') || r.includes('MANAGER'));
    }

    const profileRole = String(
      this.userService.currentUser()?.roleName ??
      this.userService.currentUser()?.role?.name ??
      ''
    ).toUpperCase();
    return profileRole.includes('ADMIN') || profileRole.includes('MANAGER');
  });
}
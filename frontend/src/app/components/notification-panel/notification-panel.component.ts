import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { NotificationService } from '../../services/notifications/notification.service';
import { AppNotification } from '../../models/notification.model';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, TranslateModule],
  template: `
    <div class="notif-panel">
      <div class="notif-head">
        <h3>Notifications</h3>
        @if (notificationService.unreadCount() > 0) {
          <button mat-button (click)="markAllRead()">Tout lire</button>
        }
      </div>

      <ul class="notif-list">
        @for (n of notificationService.notifications(); track n.id) {
          <li [class.unread]="!n.read" (click)="open(n)">
            <div class="notif-title">{{ n.title }}</div>
            @if (n.body) { <div class="notif-body">{{ n.body }}</div> }
            <div class="notif-date">{{ n.createdAt | date:'short' }}</div>
          </li>
        } @empty {
          <li class="empty">Aucune notification</li>
        }
      </ul>
    </div>
  `,
  styles: [`
    .notif-panel { width: 360px; max-height: 480px; display: flex; flex-direction: column; }
    .notif-head { display: flex; justify-content: space-between; align-items: center; padding: .75rem 1rem; border-bottom: 1px solid #e5e7eb; }
    .notif-head h3 { margin: 0; font-size: 1rem; }
    .notif-list { list-style: none; margin: 0; padding: 0; overflow-y: auto; }
    .notif-list li { padding: .75rem 1rem; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
    .notif-list li:hover { background: #f8fafc; }
    .notif-list li.unread { background: #eef6ff; }
    .notif-title { font-weight: 600; font-size: .9rem; }
    .notif-body  { font-size: .82rem; color: #64748b; margin-top: .15rem; }
    .notif-date  { font-size: .72rem; color: #94a3b8; margin-top: .25rem; }
    .notif-list li.empty { text-align: center; color: #94a3b8; padding: 2rem 1rem; cursor: default; }
  `],
})
export class NotificationPanelComponent {
  notificationService = inject(NotificationService);
  private router = inject(Router);

  @Output() closed = new EventEmitter<void>();

  open(n: AppNotification) {
    if (!n.read) {
      this.notificationService.markOneRead(n.id).subscribe();
    }
    if (n.link) {
      this.router.navigateByUrl(n.link);
    }
    this.closed.emit();
  }

  markAllRead() {
    this.notificationService.markAllRead().subscribe();
  }
}
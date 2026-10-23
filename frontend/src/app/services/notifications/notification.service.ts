import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AppNotification } from '../../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private readonly API = `${environment.apiBaseUrl}/notifications`;

  notifications = signal<AppNotification[]>([]);
  unreadCount   = signal<number>(0);

  list(): Observable<AppNotification[]> {
    if (!isPlatformBrowser(this.platformId)) return of([]);
    return this.http.get<AppNotification[]>(this.API).pipe(
      tap(list => {
        this.notifications.set(list ?? []);
        this.unreadCount.set((list ?? []).filter(n => !n.read).length);
      })
    );
  }

  unread(): Observable<{ count: number }> {
    if (!isPlatformBrowser(this.platformId)) return of({ count: 0 });
    return this.http.get<{ count: number }>(`${this.API}/unread-count`).pipe(
      tap(r => this.unreadCount.set(r?.count ?? 0))
    );
  }

  markOneRead(id: string): Observable<void> {
    return this.http.post<void>(`${this.API}/${id}/read`, {}).pipe(
      tap(() => {
        const updated = this.notifications().map(n =>
          n.id === id ? { ...n, read: true } : n
        );
        this.notifications.set(updated);
        this.unreadCount.set(updated.filter(n => !n.read).length);
      })
    );
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${this.API}/read-all`, {}).pipe(
      tap(() => {
        this.notifications.set(this.notifications().map(n => ({ ...n, read: true })));
        this.unreadCount.set(0);
      })
    );
  }

  /** Called by RealtimeService when a STOMP message arrives on /user/queue/notifications. */
  pushIncoming(n: AppNotification) {
    this.notifications.set([n, ...this.notifications()]);
    if (!n.read) this.unreadCount.update(c => c + 1);
  }
}
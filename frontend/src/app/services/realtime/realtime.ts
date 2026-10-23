import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject, firstValueFrom } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { NotificationService } from '../notifications/notification.service';
import { ProjectCreatedEvent } from '../../models/projectCreateEvent.model';
import { ProjectCriticalEvent } from '../../models/projectCriticalEvent.model';
import { ProjectUpdatedEvent } from '../../models/projectUpdatedEvent.model';
import { AppNotification } from '../../models/notification.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private auth = inject(AuthService);
  private notificationService = inject(NotificationService);
  private platformId = inject(PLATFORM_ID);
  private readonly brokerUrl = this.resolveBrokerUrl();

  private client: Client | null = null;
  private manualDisconnect = false;
  private wantedTopics = new Set<string>();
  private subs = new Map<string, StompSubscription>();
  private userQueueSubscribed = false;

  private projectCreatedSubject  = new Subject<ProjectCreatedEvent>();
  projectCreated$ = this.projectCreatedSubject.asObservable();

  private projectCriticalSubject = new Subject<ProjectCriticalEvent>();
  projectCritical$ = this.projectCriticalSubject.asObservable();

  private projectUpdatedSubject  = new Subject<ProjectUpdatedEvent>();
  projectUpdated$ = this.projectUpdatedSubject.asObservable();

  private notificationSubject = new Subject<AppNotification>();
  notification$ = this.notificationSubject.asObservable();

  connect() {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.client) return;

    this.manualDisconnect = false;

    this.client = new Client({
      brokerURL: this.brokerUrl,
      reconnectDelay: 3000,
      beforeConnect: async () => {
        const ok = await firstValueFrom(this.auth.ensureValidAccessToken());
        const latest = this.auth.getAccessToken();
        if (!ok || !latest) {
          this.stopReconnect('no valid access token');
          throw new Error('NO_ACCESS_TOKEN');
        }
        this.client!.connectHeaders = { Authorization: `Bearer ${latest}` };
      },
    });

    this.client.onConnect = () => {
      this.wantedTopics.forEach(t => this.subscribeTopic(t));
      this.subscribeUserQueue();
    };

    this.client.onStompError = frame => {
      const details = `${frame.headers['message'] ?? ''} ${frame.body ?? ''}`.toUpperCase();
      const authError =
        details.includes('NO_TOKEN') ||
        details.includes('INVALID_TOKEN') ||
        details.includes('NOT_AUTHENTICATED') ||
        details.includes('ACCESS_DENIED') ||
        details.includes('DEST_NOT_ALLOWED') ||
        details.includes('BU_NOT_ALLOWED');
      if (authError) this.stopReconnect(`stomp auth error: ${details}`);
    };

    this.client.onWebSocketError = evt => console.warn('WS error', evt);
    this.client.onWebSocketClose = evt => {
      if (this.manualDisconnect) return;
      if (!this.auth.getAccessToken()) this.stopReconnect('websocket closed without access token');
    };

    this.client.activate();
  }

  watchBuProjectsCreated(buId: string) {
    const topic = `/topic/bu/${buId}/projects.created`;
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }
  watchAllProjectsCreated() {
    const topic = '/topic/projects.created';
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }

  watchBuProjectsCritical(buId: string) {
    const topic = `/topic/bu/${buId}/projects.critical`;
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }
  watchAllProjectsCritical() {
    const topic = '/topic/projects.critical';
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }

  watchBuProjectsUpdated(buId: string) {
    const topic = `/topic/bu/${buId}/projects.updated`;
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }
  watchAllProjectsUpdated() {
    const topic = '/topic/projects.updated';
    this.wantedTopics.add(topic);
    this.connect();
    this.subscribeTopic(topic);
  }

  clearBuSubscriptions() {
    this.subs.forEach(s => s.unsubscribe());
    this.subs.clear();
    this.wantedTopics.clear();
    this.userQueueSubscribed = false;
  }

  disconnect() {
    this.manualDisconnect = true;
    this.clearBuSubscriptions();
    const client = this.client;
    if (!client) return;
    void client.deactivate().finally(() => {
      if (this.client === client) this.client = null;
    });
  }

  private subscribeUserQueue() {
    if (!this.client?.connected || this.userQueueSubscribed) return;
    const sub = this.client.subscribe('/user/queue/notifications', (msg: IMessage) => {
      try {
        const n = JSON.parse(msg.body) as AppNotification;
        this.notificationService.pushIncoming(n);
        this.notificationSubject.next(n);
      } catch (e) { console.warn('Bad notification payload', msg.body, e); }
    });
    this.subs.set('/user/queue/notifications', sub);
    this.userQueueSubscribed = true;
  }

  private subscribeTopic(topic: string) {
    if (!this.client?.connected || this.subs.has(topic)) return;

    const isCritical = topic.includes('projects.critical');
    const isUpdated  = topic.includes('projects.updated');

    const sub = this.client.subscribe(topic, (msg: IMessage) => {
      try {
        const payload = JSON.parse(msg.body);
        if (isCritical)      this.projectCriticalSubject.next(payload);
        else if (isUpdated)  this.projectUpdatedSubject.next(payload);
        else                 this.projectCreatedSubject.next(payload);
      } catch { /* ignore */ }
    });

    this.subs.set(topic, sub);
  }

  private stopReconnect(reason: string) {
    const client = this.client;
    if (!client) return;
    console.warn('Realtime reconnect stopped:', reason);
    client.reconnectDelay = 0;
    this.manualDisconnect = true;
    void client.deactivate().finally(() => {
      if (this.client === client) this.client = null;
    });
  }

  private resolveBrokerUrl(): string {
    const wsBase = environment.wsBaseUrl.trim().replace(/\/+$/, '');
    if (wsBase) return `${wsBase}/ws`;
    const apiBase = environment.apiBaseUrl.trim().replace(/\/+$/, '');
    if (apiBase.startsWith('https://')) return `wss://${apiBase.slice('https://'.length)}/ws`;
    if (apiBase.startsWith('http://'))   return `ws://${apiBase.slice('http://'.length)}/ws`;
    if (isPlatformBrowser(this.platformId)) {
      const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
      return `${wsProtocol}://${window.location.host}/ws`;
    }
    return 'ws://localhost:8080/ws';
  }
}
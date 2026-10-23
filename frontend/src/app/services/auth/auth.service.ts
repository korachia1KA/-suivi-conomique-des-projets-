import { Injectable, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, map, catchError, finalize, shareReplay } from 'rxjs';
import { Router } from '@angular/router';
import { Signup } from '../../models/signup.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root'})
export class AuthService {
 
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  
  private readonly API_URL = `${environment.apiBaseUrl}/auth`;
  private refreshInFlight$: Observable<boolean> | null = null;

  currentUserSig = signal<boolean>(false);
  currentRolesSig = signal<string[]>([]);
  resetEmailSig = signal<string| null>(null);

  setResetEmail(email: string) {
    this.resetEmailSig.set(email);
  }

  clearResetEmail() {
    this.resetEmailSig.set(null);
  }

  constructor() {
   if (isPlatformBrowser(this.platformId)) {
      const token =
        sessionStorage.getItem('accessToken') ||
        localStorage.getItem('accessToken');
      const effectiveRoles = this.extractRolesFromToken(token);
      const tokenValid = this.isAccessTokenValid();

      this.currentUserSig.set(tokenValid);
      this.currentRolesSig.set(tokenValid ? effectiveRoles : []);
    }
  }

  signup(data: Signup): Observable<any> {
    const lang = isPlatformBrowser(this.platformId) ? localStorage.getItem('lang') || 'it' : 'it';
    return this.http.post<any>(`${this.API_URL}/signup`, {...data, lang});
  }

  confirm(token: string): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/confirm`, { token });
  }
  
  login(credentials: any): Observable<any> {
    const { email, password, rememberMe } = credentials;

    return this.http.post<any>(`${this.API_URL}/login`, { email, password, rememberMe: !!rememberMe }, {
      withCredentials: true
    }).pipe(
      tap((response) => this.saveData(response, !!rememberMe))
    );
  }

  getAccessToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  }

  isAccessTokenValid(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;

    const payload = this.decodeJwtPayload(token);
    if (!payload) return false;

    const exp = Number(payload['exp']);
    if (!Number.isFinite(exp) || exp <= 0) return false;

    return Date.now() < exp * 1000;
  }

  isLoggedIn(): boolean {
    return this.isAccessTokenValid();
  }

  ensureValidAccessToken(): Observable<boolean> {
    if (!isPlatformBrowser(this.platformId)) return of(true);

    if (this.isAccessTokenValid()) return of(true);

    const existingToken = this.getAccessToken();
    if (!existingToken) return of(false);

    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.refresh().pipe(
        map((res) => res.accessToken),
        tap((newToken) => this.setAccessToken(newToken)),
        map(() => true),
        catchError(() => {
          this.clearClientSession(false);
          return of(false);
        }),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay(1)
      );
    }

    return this.refreshInFlight$;
  }

  private saveData(response: any, rememberMe: boolean) {
    
    if (!isPlatformBrowser(this.platformId)) return;

    const storage = rememberMe ? localStorage : sessionStorage;
    const other = rememberMe ? sessionStorage : localStorage;

    other.removeItem('accessToken');
    other.removeItem('userEmail');
    other.removeItem('userRoles');

    const rolesFromToken = this.extractRolesFromToken(response.accessToken);
    const effectiveRoles = rolesFromToken.length > 0
      ? rolesFromToken
      : (Array.isArray(response.roles) ? response.roles.map((r: unknown) => String(r)) : []);

    storage.setItem('accessToken', response.accessToken);
    storage.setItem('userEmail', response.email);

    this.currentUserSig.set(true);
    this.currentRolesSig.set(effectiveRoles);
  }

  logout(): void {
    this.http.post(`${this.API_URL}/logout`, {}, { withCredentials: true })
      .subscribe({
        next: () => {
          this.clearClientSession();
        },
        error: () => {
          this.clearClientSession();
        }
      });
  }

  private clearClientSession(navigateToLogin = true) {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('userEmail');
    sessionStorage.removeItem('userRoles');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRoles');
    this.currentUserSig.set(false);
    this.currentRolesSig.set([]);
    if (navigateToLogin) {
      this.router.navigate(['/auth/login']);
    }
  }

  forgotPassword(email: string): Observable<any> {
    const lang = isPlatformBrowser(this.platformId) ? localStorage.getItem('lang') || 'it' : 'it';
    return this.http.post<any>(`${this.API_URL}/forgot-password`, { email, lang }, { withCredentials: true });
  }

  resetPassword(payload: { resetToken: string; newPassword: string; confirmNewPassword: string}): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/reset-password`, payload);
  }


  verifyOtp(email: string, otp: string) {
    return this.http.post<{ resetToken: string }>(`${this.API_URL}/verify-otp`, { email, otp });
  }

  refresh(): Observable<{ accessToken: string }> {
    return this.http.post<{ accessToken: string }>(
      `${this.API_URL}/refresh`,
      {},
      { withCredentials: true } 
    );
  }

  setAccessToken(accessToken: string) {
    if (!isPlatformBrowser(this.platformId)) return;

    const hasLocal = !!localStorage.getItem('accessToken');
    const storage = hasLocal ? localStorage : sessionStorage;

    storage.setItem('accessToken', accessToken);
    this.currentUserSig.set(true);

    const rolesFromToken = this.extractRolesFromToken(accessToken);
    this.currentRolesSig.set(rolesFromToken);
  }

  getRoles(): string[] {
    const tokenRoles = this.extractRolesFromToken(this.getAccessToken());
    const roles = tokenRoles.length > 0 ? tokenRoles : (this.currentRolesSig() ?? []);
    return roles.map(r => String(r).toUpperCase());
  }

  hasRole(role: string): boolean {
    const wanted = role.toUpperCase().replace(/^ROLE_/, '');
    return this.getRoles()
      .map(r => r.toUpperCase().replace(/^ROLE_/, ''))
      .some(r => r === wanted);
  }

  private decodeJwtPayload(token: string | null): Record<string, unknown> | null {
    try {
      if (!token) return null;
      const payloadPart = token.split('.')[1];
      if (!payloadPart) return null;

      const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
      const payloadJson = atob(padded);
      return JSON.parse(payloadJson) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private extractRolesFromToken(token: string | null): string[] {
    const payload = this.decodeJwtPayload(token);
    if (!payload) return [];

    const directRoles = payload['roles'];
    if (Array.isArray(directRoles)) {
      return directRoles
        .map(r => {
          if (typeof r === 'string') return r;
          if (typeof r === 'object' && r !== null) {
            return String((r as Record<string, unknown>)['name'] ?? (r as Record<string, unknown>)['authority'] ?? '');
          }
          return '';
        })
        .filter(Boolean);
    }

    const authorities = payload['authorities'];
    if (Array.isArray(authorities)) {
      return authorities
        .map(a => {
          if (typeof a === 'string') return a;
          if (typeof a === 'object' && a !== null) {
            return String((a as Record<string, unknown>)['authority'] ?? (a as Record<string, unknown>)['name'] ?? '');
          }
          return '';
        })
        .filter(Boolean);
    }

    const singleRole = payload['role'];
    if (typeof singleRole === 'string' && singleRole.trim()) {
      return [singleRole];
    }

    return [];
  }
}
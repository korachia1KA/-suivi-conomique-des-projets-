import { inject, PLATFORM_ID } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';

let refresh$: Observable<string> | null = null;

export function authInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const platformId = inject(PLATFORM_ID);
  const authService = inject(AuthService);
  const isAuthEndpoint = req.url.includes('/auth/');

  const token = isPlatformBrowser(platformId) ? authService.getAccessToken() : null;

  const authReq = (token && !isAuthEndpoint)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {

      if (err.status === 401 && req.url.includes('/user/profile/change-password')) {
        return throwError(() => err);
      }

      if (err.status !== 401) {
        return throwError(() => err);
      }

      if (isAuthEndpoint) {
        return throwError(() => err);
      }

      if (!isPlatformBrowser(platformId)) {
        return throwError(() => err);
      }

      if (!refresh$) {
        refresh$ = authService.refresh().pipe(
          map((res) => res.accessToken),
          tap((newToken) => authService.setAccessToken(newToken)),
          shareReplay(1),
          finalize(() => {
            refresh$ = null;
          })
        );
      }

      return refresh$.pipe(
        switchMap((newToken) => {
          const retryReq = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
          return next(retryReq);
        }),
        catchError((refreshErr) => {
          authService.logout(); 
          return throwError(() => refreshErr);
        })
      );
    })
  );
}
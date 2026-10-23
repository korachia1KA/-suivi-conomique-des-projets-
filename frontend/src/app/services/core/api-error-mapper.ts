import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApiErrorMapperService {

  map(err: any): string {

    if (err?.status === 0) {
      return 'NETWORK_ERROR';
    }

    const message = (err?.message || '').toLowerCase();
    if (message.includes('failed to fetch')) {
      return 'NETWORK_ERROR';
    }

    if (err?.error?.message) {
      return err.error.message;
    }

    return 'GENERIC';
  }
}
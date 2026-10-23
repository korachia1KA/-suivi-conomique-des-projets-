import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth-interceptor';

describe('AuthInterceptor', () => {
  let service: any;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = authInterceptor;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
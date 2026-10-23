import { TestBed } from '@angular/core/testing';

import { ApiErrorMapperService } from './api-error-mapper';

describe('ApiErrorMapperService', () => {
  let service: ApiErrorMapperService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ApiErrorMapperService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { UserComponent } from './user';
import { testingProviders } from '../../../test-utils/testing.providers';
import { UserService } from '../../services/user/user';
import { UserProfile } from '../../models/userProfile.model';

const mockProfile: UserProfile = {
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  birthDay: '2000-01-01',
  roleName: 'EMPLOYEE',
};

describe('User', () => {
  let component: UserComponent;
  let fixture: ComponentFixture<UserComponent>;
  const userServiceMock = {
    getMe: vi.fn().mockReturnValue(of(mockProfile)),
    updatePassword: vi.fn().mockReturnValue(of(undefined)),
    updatePersonal: vi.fn().mockReturnValue(of(undefined)),
    updateResidence: vi.fn().mockReturnValue(of(undefined)),
    updateContacts: vi.fn().mockReturnValue(of(undefined)),
    updateBank: vi.fn().mockReturnValue(of(undefined)),
    updateLocalName: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserComponent],
      providers: [
        ...testingProviders,
        { provide: UserService, useValue: userServiceMock },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

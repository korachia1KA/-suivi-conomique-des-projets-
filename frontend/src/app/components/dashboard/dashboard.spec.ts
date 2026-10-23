import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { Dashboard } from './dashboard';
import { testingProviders } from '../../../test-utils/testing.providers';
import { DashboardService } from '../../services/dashboard/dashboard';
import { AuthService } from '../../services/auth/auth.service';
import { UserService } from '../../services/user/user';
import { RealtimeService } from '../../services/realtime/realtime';
import { DashboardResponse } from '../../models/dashboard.model';
import { UserProfile } from '../../models/userProfile.model';

const mockDashboardResponse: DashboardResponse = {
  userRole: 'EMPLOYEE',
  businessUnitName: null,
  seniorityLevel: null,
  totalProjects: 0,
  activeProjects: 0,
  scheduledProjects: 0,
  totalRevenue: 0,
  totalCost: 0,
  averageMargin: 0,
  expectedRevenue: 0,
  expectedCost: 0,
  expectedMargin: 0,
  topProjects: [],
  topBusinessUnits: [],
  projectRisk: { ok: 0, warning: 0, critical: 0 },
  businessUnitRisk: { ok: 0, warning: 0, critical: 0 },
  businessUnits: [],
  employees: [],
  upcomingDeadlines: [],
  deadlineCounts: { overdue: 0, due7: 0, due30: 0 },
  projects: [],
};

const mockProfile: UserProfile = {
  email: 'test@example.com',
  roleName: 'EMPLOYEE',
};

registerLocaleData(localeIt);

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  const projectCreated$ = new Subject<unknown>();
  const dashboardServiceMock = {
    getDashboard: vi.fn().mockReturnValue(of(mockDashboardResponse)),
  };
  const authServiceMock = {
    hasRole: vi.fn().mockReturnValue(false),
  };
  const userServiceMock = {
    loadProfile: vi.fn(),
    getMe: vi.fn().mockReturnValue(of(mockProfile)),
    currentUser: signal<UserProfile | null>(mockProfile),
  };
  const realtimeServiceMock = {
    projectCreated$: projectCreated$.asObservable(),
    watchAllProjectsCreated: vi.fn(),
    watchBuProjectsCreated: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        ...testingProviders,
        { provide: DashboardService, useValue: dashboardServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: RealtimeService, useValue: realtimeServiceMock },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
import { Routes } from '@angular/router';
import { authGuard } from './services/auth/auth-guard';

export const routes: Routes = [
  // ---- Public / auth ----
  {
    path: 'auth/login',
    title: 'Login',
    loadComponent: () => import('./components/login/login.component').then((m) => m.Login),
  },
  {
    path: 'auth/signup',
    title: 'Signup',
    loadComponent: () =>
      import('./components/signup/signup.component').then((m) => m.SignupComponent),
  },
  {
    path: 'auth/forgot-password',
    title: 'Forgot Password',
    loadComponent: () =>
      import('./components/forgot-password/forgot-password').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'auth/reset-password',
    title: 'Reset Password',
    loadComponent: () =>
      import('./components/reset-password/reset-password').then((m) => m.ResetPasswordComponent),
  },
  {
    path: 'confirm',
    title: 'Confirm Account',
    loadComponent: () =>
      import('./components/confirm-account/confirm-account.component').then(
        (m) => m.ConfirmAccountComponent,
      ),
  },

  // ---- Protected ----
  {
    path: 'dashboard',
    title: 'Dashboard',
    loadComponent: () => import('./components/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [authGuard],
  },

  // ⚠️ IMPORTANT: 'projects/create' DOIT venir AVANT 'projects/:id'
  {
    path: 'projects',
    title: 'Projects',
    loadComponent: () =>
      import('./components/projects/projects').then((m) => m.ProjectsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'projects/create',
    title: 'Create Project',
    loadComponent: () =>
      import('./components/projects/project-create/project-create').then((m) => m.ProjectCreate),
    canActivate: [authGuard],
  },
  {
    path: 'timesheet',
    title: 'Timesheet',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/timesheet/timesheet.component').then(m => m.TimesheetComponent),
  },
  {
    path: 'admin/users',
    title: 'User Management',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/admin/admin-users/admin-users.component').then(m => m.AdminUsersComponent),
  },
  {
    path: 'projects/:id',
    title: 'Project Detail',
    loadComponent: () =>
      import('./components/project-detail/project-detail.component').then(
        (m) => m.ProjectDetailComponent,
      ),
    canActivate: [authGuard],
  },

  {
    path: 'employees',
    title: 'Employees',
    loadComponent: () =>
      import('./components/employee/employee').then((m) => m.EmployeesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'costs',
    title: 'Costs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/costs/costs.component').then((m) => m.CostsComponent),
  },
  {
    path: 'user/profile',
    title: 'User Profile',
    loadComponent: () => import('./components/user/user').then((m) => m.UserComponent),
    canActivate: [authGuard],
  },

  // ---- Redirects ----
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'home', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' },
];
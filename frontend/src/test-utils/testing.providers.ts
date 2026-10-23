import { Component, EnvironmentProviders, Provider } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { Routes, provideRouter } from '@angular/router';
import {
  TranslateNoOpLoader,
  provideTranslateLoader,
  provideTranslateService,
} from '@ngx-translate/core';

@Component({
  standalone: true,
  template: '',
})
class DummyRouteComponent {}

const testingRoutes: Routes = [
  { path: 'auth/login', component: DummyRouteComponent },
  { path: 'auth/signup', component: DummyRouteComponent },
  { path: 'auth/forgot-password', component: DummyRouteComponent },
  { path: 'auth/reset-password', component: DummyRouteComponent },
  { path: 'dashboard', component: DummyRouteComponent },
  { path: 'projects', component: DummyRouteComponent },
  { path: 'projects/create', component: DummyRouteComponent },
  { path: 'user/profile', component: DummyRouteComponent },
  { path: 'confirm', component: DummyRouteComponent },
  { path: '**', component: DummyRouteComponent },
];

export const testingProviders: Array<Provider | EnvironmentProviders> = [
  provideRouter(testingRoutes),
  provideHttpClient(),
  ...provideTranslateService({
    loader: provideTranslateLoader(TranslateNoOpLoader),
    fallbackLang: 'it',
    lang: 'it',
  }),
];
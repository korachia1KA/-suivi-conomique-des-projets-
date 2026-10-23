import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import localeIt from '@angular/common/locales/it';
import localeEn from '@angular/common/locales/en';
import { registerLocaleData } from '@angular/common';

registerLocaleData(localeIt, 'it-IT');
registerLocaleData(localeEn, 'en-US');

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
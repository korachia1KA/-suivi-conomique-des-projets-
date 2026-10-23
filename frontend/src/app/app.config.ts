import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { routes } from './app.routes';
import { authInterceptor } from './services/auth/auth-interceptor';
import { I18nMatPaginatorIntl } from './services/core/mat-paginator-intl';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),

    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: './i18n/',
        suffix: '.json'
      }),
      fallbackLang: 'it',
      lang: 'it',
    }),

    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    { provide: MatPaginatorIntl, useClass: I18nMatPaginatorIntl }
  ]
};
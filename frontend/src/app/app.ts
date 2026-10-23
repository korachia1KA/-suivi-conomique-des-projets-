import { Component, signal, inject, PLATFORM_ID, HostListener } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TranslateService, TranslateModule } from '@ngx-translate/core';
import { Navbar } from "./components/navbar/navbar";
import { filter } from 'rxjs';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FooterComponent } from "./components/footer/footer";
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TranslateModule, Navbar, CommonModule, FooterComponent, MatIconModule, MatButtonModule],
  templateUrl: './app.html',
  standalone: true,
  styleUrls: ['./app.scss']
})

export class App {

  protected readonly title = signal('projectFE');
  hideNavbar = false;
  showBackToTop = signal(false);
  backToTopBottom = signal(24);

  private router = inject(Router);
  private translate = inject(TranslateService);
  private platformId = inject(PLATFORM_ID);

  constructor() {
  
    this.translate.addLangs(['it', 'en']);
    this.translate.setFallbackLang('it');
    

     if (isPlatformBrowser(this.platformId)) {
      const saved = localStorage.getItem('lang');
      const browser = this.translate.getBrowserLang();
      const initial = saved ?? (browser === 'en' ? 'en' : 'it');
      this.translate.use(initial);
    } else {
      
      this.translate.use('it');
    }

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        const url = event.urlAfterRedirects;

        this.hideNavbar =
          url.startsWith('/auth/login') ||
          url.startsWith('/auth/signup') ||
          url.startsWith('/auth/confirm') ||
          url.startsWith('/auth/forgot-password') ||
          url.startsWith('/auth/reset-password') ||
          url.startsWith('/confirm');

        if (isPlatformBrowser(this.platformId)) {
          this.updateBackToTopState();
        }
      });
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.updateBackToTopState();
  }

  scrollToTop() {
    if (!isPlatformBrowser(this.platformId)) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private updateBackToTopState() {
    const y = window.scrollY || document.documentElement.scrollTop || 0;
    this.showBackToTop.set(!this.hideNavbar && y > 220);

    const footer = document.querySelector('app-footer .footer') as HTMLElement | null;
    if (!footer) {
      this.backToTopBottom.set(24);
      return;
    }

    const footerTop = footer.getBoundingClientRect().top;
    const overlap = Math.max(0, window.innerHeight - footerTop + 12);
    this.backToTopBottom.set(24 + overlap);
  }
}
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, TranslateModule, MatButtonModule],
  templateUrl: './footer.html',
  styleUrl: './footer.scss'
})
export class FooterComponent {

  translate = inject(TranslateService);

  year = new Date().getFullYear();

  setLang(lang: string) {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
  }

  get currentLang() {
    return this.translate.currentLang || 'it';
  }
}
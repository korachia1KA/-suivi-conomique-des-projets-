import { Injectable, OnDestroy, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

@Injectable()
export class I18nMatPaginatorIntl extends MatPaginatorIntl implements OnDestroy {
  private translate = inject(TranslateService);
  private sub: Subscription;

  constructor() {
    super();
    this.translateLabels();
    this.sub = this.translate.onLangChange.subscribe(() => {
      this.translateLabels();
    });
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0 || pageSize === 0) {
      return `0 ${this.translate.instant('COMMON.PAGINATOR.OF')} ${length}`;
    }

    const startIndex = page * pageSize;
    const endIndex = Math.min(startIndex + pageSize, length);

    return `${startIndex + 1} - ${endIndex} ${this.translate.instant('COMMON.PAGINATOR.OF')} ${length}`;
  };

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private translateLabels(): void {
    this.itemsPerPageLabel = this.translate.instant('COMMON.PAGINATOR.ITEMS_PER_PAGE');
    this.nextPageLabel = this.translate.instant('COMMON.PAGINATOR.NEXT_PAGE');
    this.previousPageLabel = this.translate.instant('COMMON.PAGINATOR.PREVIOUS_PAGE');
    this.firstPageLabel = this.translate.instant('COMMON.PAGINATOR.FIRST_PAGE');
    this.lastPageLabel = this.translate.instant('COMMON.PAGINATOR.LAST_PAGE');
    this.changes.next();
  }
}
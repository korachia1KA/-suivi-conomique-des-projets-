import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmAccountComponent } from './confirm-account.component';
import { testingProviders } from '../../../test-utils/testing.providers';

describe('ConfirmAccountComponent', () => {
  let component: ConfirmAccountComponent;
  let fixture: ComponentFixture<ConfirmAccountComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmAccountComponent],
      providers: [...testingProviders],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfirmAccountComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
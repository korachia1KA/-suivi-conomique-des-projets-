import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectCreate } from './project-create';
import { testingProviders } from '../../../../test-utils/testing.providers';

describe('ProjectCreate', () => {
  let component: ProjectCreate;
  let fixture: ComponentFixture<ProjectCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectCreate],
      providers: [...testingProviders],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProjectCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
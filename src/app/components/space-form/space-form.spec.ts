import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpaceForm } from './space-form';

describe('SpaceForm', () => {
  let component: SpaceForm;
  let fixture: ComponentFixture<SpaceForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpaceForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpaceForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

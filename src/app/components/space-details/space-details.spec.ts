import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpaceDetails } from './space-details';

describe('SpaceDetails', () => {
  let component: SpaceDetails;
  let fixture: ComponentFixture<SpaceDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpaceDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpaceDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

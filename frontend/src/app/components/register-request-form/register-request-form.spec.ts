import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterRequestForm } from './register-request-form';

describe('RegisterRequestForm', () => {
  let component: RegisterRequestForm;
  let fixture: ComponentFixture<RegisterRequestForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterRequestForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterRequestForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

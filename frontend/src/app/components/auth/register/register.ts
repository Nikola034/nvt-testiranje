import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { map, Subject, takeUntil } from 'rxjs';

// PrimeNG Standalone Imports
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../services/auth/auth-service';
import { zxcvbn } from '@zxcvbn-ts/core';

// Custom Validators
function passwordMatchValidator(
  control: AbstractControl
): { [key: string]: any } | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  if (password && confirmPassword && password.value !== confirmPassword.value) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    CheckboxModule,
    ToastModule,
    ProgressSpinnerModule,
    DatePickerModule,
  ],
  providers: [MessageService],
  templateUrl: 'register.html',
})
export class Register implements OnInit, OnDestroy {
  registerForm!: FormGroup;
  isLoading = false;
  currentYear = new Date().getFullYear();
  maxDate = new Date();
  minDate = new Date(1);

  passwordStrength: 'weak' | 'medium' | 'strong' | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private messageService: MessageService,
    private authService: AuthService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.messageService.clear();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPasswordInput(event: any) {
    const value = event.target.value;
    if (!value) {
      this.passwordStrength = null;
      return;
    }

    const result = zxcvbn(value);
    if (result.score < 2) {
      this.passwordStrength = 'weak';
    } else if (result.score === 2) {
      this.passwordStrength = 'medium';
    } else {
      this.passwordStrength = 'strong';
    }
  }

  private initializeForm(): void {
    this.registerForm = this.formBuilder.group(
      {
        name: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.pattern(/^[a-zA-ZÀ-ÿ\s'-]+$/),
          ],
        ],
        surname: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.pattern(/^[a-zA-ZÀ-ÿ\s'-]+$/),
          ],
        ],
        organization: [
          '',
          [
            Validators.required,
            Validators.minLength(3),
            Validators.pattern(/^[a-zA-Z0-9_-]+$/),
          ],
        ],
        username: ['', [Validators.required, Validators.email]],
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.maxLength(64),
            this.passwordStrengthValidator,
          ]
        ],
        confirmPassword: ['', [Validators.required]],
      },
      {
        validators: passwordMatchValidator,
      }
    );
  }


  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.value;
    if (!password) return null;

    const result = zxcvbn(password);

    if (result.score < 2) {
      return { weakPassword: true };
    }

    return null;
  }

  onSubmit(): void {
    console.log(
      'Invalid fields:',
      Object.keys(this.registerForm.controls).filter(
        (key) => this.registerForm.get(key)?.invalid
      )
    );

    if (this.registerForm.invalid) {
      this.markFormGroupTouched();
      this.showValidationErrors();

      return;
    }

    this.performRegistration();
  }

  private performRegistration(): void {
    this.isLoading = true;
    this.messageService.clear();

    const formValue = this.registerForm.value;
    const registrationData = {
      username: formValue.username.trim(),
      password: formValue.password.trim(),
      name: formValue.name.trim(),
      surname: formValue.surname.trim(),
      phoneNumber: formValue.phoneNumber,
    };

    this.authService
      .register(registrationData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Registration Successful!',
            detail:
              'Your account has been created. You have to activate your account.',
            life: 5000,
          });
          this.isLoading = false;
        },
        error: (error) => {
          this.isLoading = false;
          this.handleRegistrationError(error);
        },
        complete: () => {
          this.isLoading = false;
        },
      });
  }

  private handleRegistrationError(error: any): void {
    let errorMessage = 'Registration failed';
    let errorDetails: string[] = [];

    // Handle server validation errors with details array
    if (
      error.error &&
      error.error.details &&
      Array.isArray(error.error.details)
    ) {
      errorMessage = error.error.error || 'Validation failed';
      errorDetails = error.error.details;
    } else {
      // Handle other error types
      switch (error.status) {
        case 409:
          errorMessage = 'Username/Email Already Exists';
          errorDetails = ['Please choose different credentials.'];
          break;
        case 400:
          errorMessage = 'Invalid Information';
          errorDetails = ['Please check your input and try again.'];
          break;
        default:
          errorMessage = 'Registration Failed';
          errorDetails = ['Please try again later.'];
      }
    }

    // Display error with details
    this.messageService.add({
      severity: 'error',
      summary: errorMessage,
      detail: errorDetails.join('\n'),
      life: 8000,
    });
  }

  private markFormGroupTouched(): void {
    Object.keys(this.registerForm.controls).forEach((key) => {
      const control = this.registerForm.get(key);
      control?.markAsTouched();
    });
  }

  private showValidationErrors(): void {
    const invalidFields = this.getInvalidFields();
    const formErrors = this.registerForm.errors || {};

    // Collect all validation errors
    const errorDetails: string[] = [];

    // Add field-specific errors
    invalidFields.forEach((field) => {
      const fieldDisplayName = this.getFieldDisplayName(field.field);
      const errorMessage = this.getFirstErrorMessage(field.errors);
      errorDetails.push(`${fieldDisplayName}: ${errorMessage}`);
    });

    // Add form-level errors
    if (formErrors['passwordMismatch']) {
      errorDetails.push('Passwords do not match');
    }

    // Display validation errors
    if (errorDetails.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Failed',
        detail: errorDetails.join('\n'),
        life: 8000,
      });
    }
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      username: 'Email',
      password: 'Password',
      confirmPassword: 'Confirm Password',
      name: 'Name',
      surname: 'Surname',
      organization: 'Organization',
    };
    return displayNames[fieldName] || fieldName;
  }

  private getFirstErrorMessage(errors: any): string {
    if (errors.required) return 'is required';
    if (errors.email) return 'must be a valid email address';
    if (errors.minlength)
      return `must be at least ${errors.minlength.requiredLength} characters`;
    if (errors.pattern) return 'format is invalid';
    if (errors.requiredTrue) return 'must be accepted';
    if (errors.tooYoung) return 'must be at least 13 years old';
    if (errors.tooOld) return 'must be a valid date';
    return 'is invalid';
  }

  private getInvalidFields(): { field: string; errors: any }[] {
    const invalidFields: { field: string; errors: any }[] = [];

    Object.keys(this.registerForm.controls).forEach((key) => {
      const control = this.registerForm.get(key);
      if (control && control.invalid) {
        invalidFields.push({
          field: key,
          errors: control.errors,
        });
      }
    });

    return invalidFields;
  }

  navigateToLogin(): void {
    this.router.navigate(['/']);
  }

  // Getters for template
  get userNameControl() {
    return this.registerForm.get('username');
  }
  get passwordControl() {
    return this.registerForm.get('password');
  }
  get confirmPasswordControl() {
    return this.registerForm.get('confirmPassword');
  }
  get nameControl() {
    return this.registerForm.get('name');
  }
  get surnameControl() {
    return this.registerForm.get('surname');
  }
  get organizationControl() {
    return this.registerForm.get('organization');
  }

  // Validation state getters
  get isNameInvalid(): boolean {
    const control = this.nameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isSurnameInvalid(): boolean {
    const control = this.surnameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isOrganizationInvalid(): boolean {
    const control = this.organizationControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isUsernameInvalid(): boolean {
    const control = this.userNameControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isPasswordInvalid(): boolean {
    const control = this.passwordControl;
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  get isConfirmPasswordInvalid(): boolean {
    const control = this.confirmPasswordControl;
    return (
      !!(control && control.invalid && (control.dirty || control.touched)) ||
      this.registerForm.errors?.['passwordMismatch']
    );
  }

  // Error message getters
  get firstNameErrorMessage(): string {
    const control = this.nameControl;
    if (control?.errors?.['required']) return 'First name is required';
    if (control?.errors?.['minlength'])
      return 'First name must be at least 2 characters';
    if (control?.errors?.['pattern'])
      return 'First name can only contain letters, spaces, apostrophes, and hyphens';
    return '';
  }

  get lastNameErrorMessage(): string {
    const control = this.surnameControl;
    if (control?.errors?.['required']) return 'Last name is required';
    if (control?.errors?.['minlength'])
      return 'Last name must be at least 2 characters';
    if (control?.errors?.['pattern'])
      return 'Last name can only contain letters, spaces, apostrophes, and hyphens';
    return '';
  }

  get usernameErrorMessage(): string {
    const control = this.userNameControl;
    if (control?.errors?.['required']) return 'Username is required';
    if (control?.errors?.['minlength'])
      return 'Username must be at least 3 characters';
    if (control?.errors?.['pattern'])
      return 'Username can only contain letters, numbers, hyphens, and underscores';
    return '';
  }

  get organizationErrorMessage(): string {
    const control = this.userNameControl;
    if (control?.errors?.['required']) return 'Username is required';
    if (control?.errors?.['pattern'])
      return 'Username can only contain letters, numbers, hyphens, and underscores';
    return '';
  }

  get emailErrorMessage(): string {
    const control = this.userNameControl;
    if (control?.errors?.['required']) return 'Email is required';
    if (control?.errors?.['email']) return 'Please enter a valid email address';
    return '';
  }

  get passwordErrorMessage(): string {
    const control = this.passwordControl;
    if (control?.errors?.['required']) return 'Password is required';
    if (control?.errors?.['minlength'])
      return 'Password must be at least 8 characters';
    if (control?.errors?.['pattern'])
      return 'Password must contain at least one uppercase letter, one lowercase letter, and one number';
    return '';
  }

  get confirmPasswordErrorMessage(): string {
    const control = this.confirmPasswordControl;
    if (control?.errors?.['required']) return 'Please confirm your password';
    if (this.registerForm.errors?.['passwordMismatch'])
      return 'Passwords do not match';
    return '';
  }
}

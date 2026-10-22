
import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class RegisterComponent {

  loading = false;
  errorMessage = '';

  fieldErrors: Record<string, string> = {};

  form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', Validators.required],
      email: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  clearError(field: string): void {
    this.errorMessage = '';
    delete this.fieldErrors[field];
    this.cdr.markForCheck();
  }

  register(): void {

    if (this.loading) {
      return;
    }

    this.errorMessage = '';
    this.fieldErrors = {};

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    this.cdr.markForCheck();

    const request = this.form.getRawValue();

    this.authService.register(request)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({

        next: () => {
          this.router.navigate(['/login']);
        },

        error: (error: HttpErrorResponse) => {

          console.error('Registration failed:', error);

          const response = error.error;

          let message = 'Registration failed. Please try again.';

          if (error.status === 0) {
            message = 'Cannot connect to the server. Please try again.';
          } else if (
            typeof response === 'string' &&
            response.trim()
          ) {
            message = response.trim();
          } else if (
            typeof response?.message === 'string' &&
            response.message.trim()
          ) {
            message = response.message.trim();
          } else if (
            typeof response?.error === 'string' &&
            response.error.trim()
          ) {
            message = response.error.trim();
          }

          // Handle structured field errors returned by backend
          if (
            response &&
            typeof response === 'object' &&
            response.errors &&
            typeof response.errors === 'object' &&
            !Array.isArray(response.errors)
          ) {
            for (const [field, value] of Object.entries(response.errors)) {

              if (
                ['name', 'email', 'password'].includes(field) &&
                typeof value === 'string'
              ) {
                this.fieldErrors[field] = value;
              }
            }
          }

          // Handle plain-text backend validation messages
          if (Object.keys(this.fieldErrors).length === 0) {

            const normalized = message.toLowerCase();

            if (error.status === 400 || error.status === 409) {

              if (normalized.includes('password')) {
                this.fieldErrors['password'] = message;

              } else if (normalized.includes('email')) {
                this.fieldErrors['email'] = message;

              } else if (
                normalized.includes('name') &&
                !normalized.includes('username')
              ) {
                this.fieldErrors['name'] = message;

              } else {
                this.errorMessage = message;
              }

            } else {
              this.errorMessage = message;
            }
          }

          this.cdr.markForCheck();
        }
      });
  }
}

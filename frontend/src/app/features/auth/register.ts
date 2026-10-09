
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './register.html'
})
export class RegisterComponent {
  loading = false;
  errorMessage = '';

  form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  clearError(): void {
    this.errorMessage = '';
  }

  register(): void {
    this.errorMessage = '';

    // Frontend validation: do not send invalid form data.
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.loading = false;

        const response = error?.error;

        // Display plain-text messages returned by Spring Boot.
        if (typeof response === 'string' && response.trim()) {
          this.errorMessage = response;
        } else if (
          typeof response?.message === 'string' &&
          response.message.trim()
        ) {
          this.errorMessage = response.message;
        } else if (error?.status === 0) {
          this.errorMessage =
            'Unable to connect to the server. Please try again.';
        } else if (error?.status === 403) {
          this.errorMessage =
            'Registration was forbidden. Please check the server security configuration.';
        } else {
          this.errorMessage =
            'Registration failed. Please try again.';
        }

        console.error('Registration failed:', error);
      }
    });
  }
}

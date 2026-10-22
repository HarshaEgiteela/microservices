
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { CartService } from '../../core/services/cart.service';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html'
})
export class LoginComponent {

  email = '';
  password = '';

  loading = signal(false);
  error = signal('');
  info = signal('');

  emailError = signal('');
  passwordError = signal('');

  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    this.route.queryParamMap.subscribe(params => {
      this.info.set(params.get('message') || '');
    });
  }

  clearError(field: 'email' | 'password'): void {
    this.error.set('');

    if (field === 'email') {
      this.emailError.set('');
    } else {
      this.passwordError.set('');
    }
  }

  submit(): void {

    if (this.loading()) {
      return;
    }

    // Clear previous errors
    this.error.set('');
    this.emailError.set('');
    this.passwordError.set('');

    // Required-field validation
    if (!this.email.trim()) {
      this.emailError.set('Email is required.');
    }

    if (!this.password) {
      this.passwordError.set('Password is required.');
    }

    if (this.emailError() || this.passwordError()) {
      return;
    }

    this.loading.set(true);

    this.auth.login({
      email: this.email.trim(),
      password: this.password
    })
      .pipe(
        finalize(() => {
          this.loading.set(false);
        })
      )
      .subscribe({

        next: () => {

          const requestedUrl =
            this.route.snapshot.queryParamMap.get('returnUrl');

          const destination =
            requestedUrl &&
            requestedUrl.startsWith('/') &&
            !requestedUrl.startsWith('//')
              ? requestedUrl
              : (this.auth.isAdmin() ? '/admin' : '/');
          this.cart.load();
          this.router.navigateByUrl(destination);
        },

        error: (err: HttpErrorResponse) => {

          const response = err.error;

          let message = 'Login failed. Please try again.';

          // Extract backend error message
          if (err.status === 0) {
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

          const normalized = message.toLowerCase();

          // Invalid login credentials
          // Display ONLY below the password field
          if (
            (err.status === 400 || err.status === 401) &&
            (
              normalized.includes('invalid email or password') ||
              normalized.includes('invalid credentials') ||
              normalized.includes('incorrect credentials') ||
              normalized.includes('incorrect email or password')
            )
          ) {
            this.passwordError.set('Invalid credentials');
            return;
          }

          // Structured backend field errors
          if (
            response &&
            typeof response === 'object' &&
            response.errors &&
            typeof response.errors === 'object' &&
            !Array.isArray(response.errors)
          ) {
            if (typeof response.errors.email === 'string') {
              this.emailError.set(response.errors.email);
            }

            if (typeof response.errors.password === 'string') {
              this.passwordError.set(response.errors.password);
            }
          }

          // Plain-text backend validation errors
          if (!this.emailError() && !this.passwordError()) {

            if (
              err.status === 400 &&
              normalized.includes('email')
            ) {
              this.emailError.set(message);

            } else if (
              err.status === 400 &&
              normalized.includes('password')
            ) {
              this.passwordError.set(message);

            } else {
              // Network errors and other backend errors
              this.error.set(message);
            }
          }
        }
      });
  }
}

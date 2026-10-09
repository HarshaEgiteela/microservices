import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    this.route.queryParamMap.subscribe(params => {
      this.info.set(params.get('message') || '');
    });
  }

  submit(): void {
    this.error.set('');
    if (!this.email.trim() || !this.password) {
      this.error.set('Email and password are required.');
      return;
    }

    this.loading.set(true);
    this.auth.login({ email: this.email.trim(), password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        const requestedUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const destination = requestedUrl && requestedUrl.startsWith('/')
          ? requestedUrl
          : (this.auth.isAdmin() ? '/admin' : '/');
        this.router.navigateByUrl(destination);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(typeof err?.error === 'string'
          ? err.error
          : 'Login failed. Check your credentials.');
      }
    });
  }
}

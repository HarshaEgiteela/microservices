
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest
} from '../models/app.models';

import { API_BASE_URL } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly tokenKey = 'token';
  private readonly roleKey = 'role';
  private readonly userIdKey = 'userId';

  private readonly tokenState = signal<string | null>(
    localStorage.getItem(this.tokenKey)
  );

  private readonly roleState = signal<string | null>(
    localStorage.getItem(this.roleKey)
  );

  constructor(private readonly http: HttpClient) {}

  // Login
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${API_BASE_URL}/users/login`,
      request
    ).pipe(
      tap(response => {
        this.storeAuth(response);
      })
    );
  }

  // Register
  register(request: RegisterRequest): Observable<string> {
    return this.http.post(
      `${API_BASE_URL}/users`,
      request,
      { responseType: 'text' }
    );
  }

  // Logout
  logout(): void {

    // Remove authentication details.
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.roleKey);
    localStorage.removeItem(this.userIdKey);

    // Update authentication signals.
    this.tokenState.set(null);
    this.roleState.set(null);
  }

  // Authentication status
  hasToken(): boolean {
    return !!this.tokenState();
  }

  getToken(): string | null {
    return this.tokenState();
  }

  // Role
  getRole(): string | null {
    return this.roleState();
  }

  isAdmin(): boolean {
    return this.roleState() === 'ADMIN';
  }

  // Logged-in user ID
  getUserId(): number | null {

    const value = localStorage.getItem(this.userIdKey);

    if (!value) {
      return null;
    }

    const id = Number(value);

    return Number.isFinite(id) ? id : null;
  }

  // Store authentication response
  private storeAuth(response: AuthResponse): void {

    const token = response.token ?? response.jwt;
    const role = response.role ?? response.user?.role;

    const userId =
      response.userId ??
      response.id ??
      response.user?.id;

    if (token) {
      localStorage.setItem(this.tokenKey, token);
      this.tokenState.set(token);
    }

    if (role) {
      localStorage.setItem(this.roleKey, role);
      this.roleState.set(role);
    }

    if (userId !== undefined && userId !== null) {
      localStorage.setItem(this.userIdKey, String(userId));
    }
  }
}

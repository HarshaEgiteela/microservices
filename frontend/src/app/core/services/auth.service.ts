
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  Observable,
  tap,
  switchMap,
  map,
  catchError,
  throwError
} from 'rxjs';

import {
  AuthResponse,
  LoginRequest,
  RegisterRequest
} from '../models/app.models';

import { API_BASE_URL } from '../config/api.config';

// Response returned by Spring Boot GET /users/me
export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly tokenKey = 'token';

  // Only the JWT is stored in sessionStorage.
  private readonly tokenState = signal<string | null>(
    sessionStorage.getItem(this.tokenKey)
  );

  // User information is stored only in Angular memory.
  private readonly userState = signal<UserProfile | null>(null);

  constructor(private readonly http: HttpClient) {

    // Remove authentication data from the old implementation.
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
  }

  // Login
  login(request: LoginRequest): Observable<AuthResponse> {

    return this.http.post<AuthResponse>(
      `${API_BASE_URL}/users/login`,
      request
    ).pipe(
      switchMap(response => {

        // Clear any previous session.
        this.logout();

        const token = response.token ?? response.jwt;

        if (!token) {
          return throwError(() =>
            new Error('Login response does not contain a token')
          );
        }

        // Store only the JWT.
        sessionStorage.setItem(this.tokenKey, token);
        this.tokenState.set(token);

        // Ask the backend to validate the JWT
        // and return the authenticated user's profile.
        return this.loadProfile().pipe(
          map(() => response)
        );
      }),

      catchError(error => {
        this.logout();
        return throwError(() => error);
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

  // Retrieve profile from Spring Boot
  loadProfile(): Observable<UserProfile> {

    return this.http.get<UserProfile>(
      `${API_BASE_URL}/users/me`
    ).pipe(
      tap(profile => {
        this.userState.set(profile);
      })
    );
  }

  // Logout
  logout(): void {

    sessionStorage.removeItem(this.tokenKey);

    this.tokenState.set(null);
    this.userState.set(null);
  }

  // Checks token presence only.
  // Actual JWT validation happens in Spring Boot.
  hasToken(): boolean {
    return !!this.tokenState();
  }

  // Get JWT for HTTP interceptor
  getToken(): string | null {
    return this.tokenState();
  }

  // Get authenticated user
  getUser(): UserProfile | null {
    return this.userState();
  }

  // Get role from backend-provided profile
  getRole(): string | null {
    return this.userState()?.role ?? null;
  }

  // Check ADMIN role for frontend UI
  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  // Get logged-in user ID
  getUserId(): number | null {
    return this.userState()?.id ?? null;
  }
}

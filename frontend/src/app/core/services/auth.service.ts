import { Injectable } from '@angular/core';
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

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${API_BASE_URL}/users/login`,
      request
    ).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  register(request: RegisterRequest): Observable<unknown> {
    return this.http.post(
      `${API_BASE_URL}/users`,
      request
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.roleKey);
    localStorage.removeItem(this.userIdKey);
  }

  hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRole(): string | null {
    return localStorage.getItem(this.roleKey);
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  getUserId(): number | null {
    const value = localStorage.getItem(this.userIdKey);
    if (!value) return null;
    const id = Number(value);
    return Number.isFinite(id) ? id : null;
  }

  private storeAuth(response: AuthResponse): void {
    const token = response.token ?? response.jwt;
    const role = response.role ?? response.user?.role;
    const userId = response.userId ?? response.id ?? response.user?.id;

    if (token) localStorage.setItem(this.tokenKey, token);
    if (role) localStorage.setItem(this.roleKey, role);
    if (userId !== undefined && userId !== null) {
      localStorage.setItem(this.userIdKey, String(userId));
    }
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthMessageResponse, LoginRequest, LoginResponse, RegisterRequest } from './auth.models';

const TOKEN_KEY = 'bookify.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenState = signal<string | null>(this.readToken());

  readonly token = this.tokenState.asReadonly();

  hasValidSession(): boolean {
    return this.authorizationToken() !== null;
  }

  authorizationToken(): string | null {
    const token = this.tokenState();
    if (!token || !this.isTokenValid(token)) {
      if (token) {
        this.logout();
      }
      return null;
    }
    return token;
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      tap(({ token }) => this.persistToken(token)),
    );
  }

  register(request: RegisterRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>('/api/auth/register', request);
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.tokenState.set(null);
  }

  private persistToken(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.tokenState.set(token);
  }

  private readToken(): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (token && this.isTokenValid(token)) {
      return token;
    }
    sessionStorage.removeItem(TOKEN_KEY);
    return null;
  }

  private isTokenValid(token: string): boolean {
    try {
      const payloadPart = token.split('.')[1];
      if (!payloadPart) {
        return false;
      }
      const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const payload = JSON.parse(atob(padded)) as { exp?: unknown };
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }
}

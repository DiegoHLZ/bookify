import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthMessageResponse, LoginRequest, LoginResponse, RegisterRequest } from './auth.models';

const TOKEN_KEY = 'bookify.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenState = signal<string | null>(this.readToken());

  readonly token = this.tokenState.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.tokenState()));

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
    return sessionStorage.getItem(TOKEN_KEY);
  }
}

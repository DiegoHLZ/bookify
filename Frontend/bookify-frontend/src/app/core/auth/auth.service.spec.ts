import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

const TOKEN_KEY = 'bookify.session';

function tokenWithExpiration(expirationSeconds: number): string {
  const payload = btoa(JSON.stringify({ exp: expirationSeconds }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${payload}.signature`;
}

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => sessionStorage.clear());

  it('restores a non-expired session', () => {
    sessionStorage.setItem(TOKEN_KEY, tokenWithExpiration(Math.floor(Date.now() / 1000) + 60));
    const service = TestBed.inject(AuthService);

    expect(service.hasValidSession()).toBe(true);
  });

  it('removes an expired session', () => {
    sessionStorage.setItem(TOKEN_KEY, tokenWithExpiration(Math.floor(Date.now() / 1000) - 60));
    const service = TestBed.inject(AuthService);

    expect(service.hasValidSession()).toBe(false);
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('rejects a malformed token', () => {
    sessionStorage.setItem(TOKEN_KEY, 'not-a-jwt');
    const service = TestBed.inject(AuthService);

    expect(service.hasValidSession()).toBe(false);
  });
});

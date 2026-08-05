import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly passwordVisible = signal(false);
  readonly registered = this.route.snapshot.queryParamMap.get('registered') === 'true';

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.auth.login(this.form.getRawValue()).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: () => {
        const requestedUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        const destination = requestedUrl?.startsWith('/') && !requestedUrl.startsWith('//')
          ? requestedUrl
          : '/panel';
        void this.router.navigateByUrl(destination);
      },
      error: (error: HttpErrorResponse) => this.errorMessage.set(this.readError(error)),
    });
  }

  private readError(error: HttpErrorResponse): string {
    const apiError = error.error as Partial<ApiError> | null;
    if (apiError?.message === 'Invalid credentials') {
      return 'El correo o la contraseña no son correctos.';
    }
    return apiError?.message ?? 'No pudimos iniciar sesión. Inténtalo nuevamente.';
  }
}

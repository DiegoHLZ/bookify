import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiError } from '../../../core/auth/auth.models';
import { BUSINESS_CATEGORIES, BusinessCategoryOption, BusinessOnboardingResponse, CreateBusinessRequest } from '../../../core/business/business.models';
import { BusinessService } from '../../../core/business/business.service';

@Component({
  selector: 'app-onboarding',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './onboarding.html',
  styleUrl: './onboarding.css',
})
export class Onboarding {
  private readonly businessService = inject(BusinessService);
  private readonly router = inject(Router);

  readonly categories = BUSINESS_CATEGORIES;
  readonly step = signal(1);
  readonly loading = signal(false);
  readonly locating = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly createdBusiness = signal<BusinessOnboardingResponse | null>(null);
  readonly selectedCategory = computed(() => this.categories.find((category) => category.code === this.businessForm.controls.categoryCode.value));

  readonly businessForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(3), Validators.maxLength(100), Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)] }),
    categoryCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.email, Validators.maxLength(150)] }),
  });

  readonly locationForm = new FormGroup({
    name: new FormControl('Sede principal', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    address: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(250)] }),
    city: new FormControl('Lima', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    countryCode: new FormControl('PE', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[A-Z]{2}$/)] }),
    timezone: new FormControl(this.localTimezone(), { nonNullable: true, validators: [Validators.required, Validators.maxLength(60)] }),
    latitude: new FormControl<number | null>(null, [Validators.required, Validators.min(-90), Validators.max(90)]),
    longitude: new FormControl<number | null>(null, [Validators.required, Validators.min(-180), Validators.max(180)]),
  });

  chooseCategory(category: BusinessCategoryOption): void {
    this.businessForm.controls.categoryCode.setValue(category.code);
  }

  updateSlug(): void {
    if (this.businessForm.controls.slug.dirty) {
      return;
    }
    this.businessForm.controls.slug.setValue(this.slugify(this.businessForm.controls.name.value));
  }

  next(): void {
    const form = this.step() === 1 ? this.businessForm : this.locationForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.step.update((value) => Math.min(3, value + 1));
  }

  previous(): void {
    this.errorMessage.set(null);
    this.step.update((value) => Math.max(1, value - 1));
  }

  locate(): void {
    if (!navigator.geolocation || this.locating()) {
      this.errorMessage.set('Tu navegador no permite obtener la ubicación. Ingresa las coordenadas manualmente.');
      return;
    }
    this.locating.set(true);
    this.errorMessage.set(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        this.locationForm.patchValue({
          latitude: Number(coords.latitude.toFixed(6)),
          longitude: Number(coords.longitude.toFixed(6)),
        });
        this.locating.set(false);
      },
      () => {
        this.errorMessage.set('No pudimos acceder a tu ubicación. Puedes ingresar las coordenadas manualmente.');
        this.locating.set(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    );
  }

  submit(): void {
    if (this.businessForm.invalid || this.locationForm.invalid || this.loading()) {
      this.businessForm.markAllAsTouched();
      this.locationForm.markAllAsTouched();
      return;
    }
    const location = this.locationForm.getRawValue();
    if (location.latitude === null || location.longitude === null) {
      return;
    }
    const business = this.businessForm.getRawValue();
    const request: CreateBusinessRequest = {
      ...business,
      name: business.name.trim(),
      slug: business.slug.trim(),
      description: business.description.trim() || null,
      phone: business.phone.trim() || null,
      email: business.email.trim() || null,
      location: { ...location, latitude: location.latitude, longitude: location.longitude },
    };

    this.loading.set(true);
    this.errorMessage.set(null);
    this.businessService.onboard(request).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (created) => {
        this.createdBusiness.set(created);
        this.step.set(4);
      },
      error: (error: HttpErrorResponse) => this.errorMessage.set(this.readError(error)),
    });
  }

  finish(): void {
    void this.router.navigate(['/panel'], { queryParams: { onboarding: 'completed' } });
  }

  private localTimezone(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'America/Lima';
  }

  private slugify(value: string): string {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()
      .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 100);
  }

  private readError(error: HttpErrorResponse): string {
    const apiError = error.error as Partial<ApiError> | null;
    if (apiError?.message?.toLowerCase().includes('slug')) {
      return 'Ese identificador ya está en uso. Prueba con uno diferente.';
    }
    if (apiError?.validationErrors) {
      return Object.values(apiError.validationErrors)[0] ?? 'Revisa los datos ingresados.';
    }
    return apiError?.message ?? 'No pudimos crear el negocio. Inténtalo nuevamente.';
  }
}

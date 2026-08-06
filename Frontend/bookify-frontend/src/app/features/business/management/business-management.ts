import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ApiError } from '../../../core/auth/auth.models';
import {
  BookingMode,
  BusinessLocation,
  MyBusiness,
  ServiceOffering,
  SupportedCurrency,
} from '../../../core/business/business.models';
import { BusinessService } from '../../../core/business/business.service';

@Component({
  selector: 'app-business-management',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './business-management.html',
  styleUrl: './business-management.css',
})
export class BusinessManagement {
  private readonly businessService = inject(BusinessService);
  private readonly route = inject(ActivatedRoute);

  readonly businessId = Number(this.route.snapshot.paramMap.get('businessId'));
  readonly business = signal<MyBusiness | null>(null);
  readonly locations = signal<BusinessLocation[]>([]);
  readonly services = signal<ServiceOffering[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly formOpen = signal(false);
  readonly canManage = computed(() => ['OWNER', 'ADMIN'].includes(this.business()?.membershipRole ?? ''));
  readonly activeLocations = computed(() => this.locations().filter((location) => location.active));

  readonly serviceForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    durationMinutes: new FormControl(60, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(1440)] }),
    price: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    currency: new FormControl<SupportedCurrency>('PEN', { nonNullable: true, validators: [Validators.required] }),
    bookingMode: new FormControl<BookingMode>('EXCLUSIVE_RESOURCE', { nonNullable: true, validators: [Validators.required] }),
    locationId: new FormControl<number | null>(null, [Validators.required]),
  });

  constructor() {
    this.load();
  }

  load(): void {
    if (!Number.isInteger(this.businessId) || this.businessId <= 0) {
      this.loadError.set('El identificador del negocio no es válido.');
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      businesses: this.businessService.listMine(),
      locations: this.businessService.listLocations(this.businessId),
      services: this.businessService.listServices(this.businessId),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ businesses, locations, services }) => {
        const business = businesses.find((item) => item.id === this.businessId) ?? null;
        if (!business) {
          this.loadError.set('No tienes acceso a este negocio.');
          return;
        }
        this.business.set(business);
        this.locations.set(locations);
        this.services.set(services);
        this.serviceForm.controls.locationId.setValue(locations.find((location) => location.active)?.id ?? null);
      },
      error: (error: HttpErrorResponse) => this.loadError.set(this.readError(error, 'No pudimos cargar la gestión del negocio.')),
    });
  }

  openForm(): void {
    this.formError.set(null);
    this.formOpen.set(true);
  }

  closeForm(): void {
    if (!this.saving()) {
      this.formOpen.set(false);
      this.formError.set(null);
    }
  }

  submit(): void {
    if (this.serviceForm.invalid || this.saving() || !this.canManage()) {
      this.serviceForm.markAllAsTouched();
      return;
    }
    const value = this.serviceForm.getRawValue();
    if (value.locationId === null) {
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    this.businessService.createService(this.businessId, {
      name: value.name.trim(),
      description: value.description.trim() || null,
      durationMinutes: value.durationMinutes,
      price: value.price,
      currency: value.currency,
      bookingMode: value.bookingMode,
      locationIds: [value.locationId],
      customerCancellationAllowed: true,
      cancellationNoticeMinutes: 120,
      customerRescheduleAllowed: true,
      rescheduleNoticeMinutes: 120,
      maxReschedules: 2,
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (created) => {
        this.services.update((services) => [...services, created]);
        this.serviceForm.reset({
          name: '', description: '', durationMinutes: 60, price: 0, currency: 'PEN',
          bookingMode: 'EXCLUSIVE_RESOURCE', locationId: value.locationId,
        });
        this.formOpen.set(false);
      },
      error: (error: HttpErrorResponse) => this.formError.set(this.readError(error, 'No pudimos crear el servicio.')),
    });
  }

  formatPrice(service: ServiceOffering): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: service.currency }).format(service.price);
  }

  bookingModeName(mode: BookingMode): string {
    return mode === 'CAPACITY_SESSION' ? 'Cupo compartido' : 'Recurso exclusivo';
  }

  private readError(error: HttpErrorResponse, fallback: string): string {
    const apiError = error.error as Partial<ApiError> | null;
    if (apiError?.validationErrors) {
      return Object.values(apiError.validationErrors)[0] ?? fallback;
    }
    return apiError?.message ?? fallback;
  }
}

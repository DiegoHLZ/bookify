import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AvailabilitySlot, Booking } from '../../../core/booking/booking.models';
import { BookingService } from '../../../core/booking/booking.service';
import { PublicBusinessDetail, PublicService } from '../../../core/discovery/discovery.models';
import { DiscoveryService } from '../../../core/discovery/discovery.service';

@Component({
  selector: 'app-booking-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './booking-create.html',
  styleUrl: './booking-create.css',
})
export class BookingCreate implements OnInit {
  private readonly bookings = inject(BookingService);
  private readonly discovery = inject(DiscoveryService);
  private readonly route = inject(ActivatedRoute);

  readonly business = signal<PublicBusinessDetail | null>(null);
  readonly service = signal<PublicService | null>(null);
  readonly locationId = signal<number | null>(null);
  readonly slots = signal<AvailabilitySlot[]>([]);
  readonly selectedSlot = signal<AvailabilitySlot | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly confirmed = signal<Booking | null>(null);
  readonly timezone = signal('');
  slug = '';
  readonly date = new FormControl(this.initialDate(), { nonNullable: true, validators: [Validators.required] });
  readonly notes = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] });
  readonly quantity = new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] });
  readonly groupedSlots = computed(() => {
    const groups = new Map<string, AvailabilitySlot[]>();
    for (const slot of this.slots()) {
      const day = slot.localStart.slice(0, 10);
      groups.set(day, [...(groups.get(day) ?? []), slot]);
    }
    return [...groups.entries()];
  });

  private businessId = 0;
  private serviceId = 0;
  private idempotencyKey = this.newKey();

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.businessId = this.numberParam(params.get('businessId'));
    this.serviceId = this.numberParam(params.get('serviceId'));
    this.locationId.set(this.numberParam(params.get('locationId')) || null);
    this.slug = params.get('slug') ?? '';
    const requestedAt = params.get('at');
    if (requestedAt && requestedAt.slice(0, 10) >= this.today()) this.date.setValue(requestedAt.slice(0, 10));

    if (!this.businessId || !this.serviceId || !this.locationId() || !this.slug) {
      this.loading.set(false);
      this.error.set('La selección de reserva está incompleta. Vuelve al negocio y elige un servicio.');
      return;
    }
    this.load();
  }

  load(): void {
    if (this.date.invalid || !this.locationId()) return;
    this.loading.set(true);
    this.error.set('');
    this.selectedSlot.set(null);
    const from = this.date.value;
    forkJoin({
      business: this.discovery.getBusiness(this.slug),
      availability: this.bookings.availability(this.businessId, this.locationId()!, this.serviceId, from, this.addDays(from, 6)),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ business, availability }) => {
        const service = business.services.find((candidate) => candidate.id === this.serviceId && candidate.locationIds.includes(this.locationId()!));
        if (!service) {
          this.error.set('Este servicio ya no está disponible en la sede seleccionada.');
          return;
        }
        this.business.set(business);
        this.service.set(service);
        this.timezone.set(availability.timezone);
        this.slots.set(availability.slots);
      },
      error: () => this.error.set('No pudimos consultar los horarios. Inténtalo nuevamente.'),
    });
  }

  select(slot: AvailabilitySlot): void {
    this.selectedSlot.set(slot);
    this.quantity.setValue(1);
    this.idempotencyKey = this.newKey();
    this.error.set('');
  }

  confirm(): void {
    const slot = this.selectedSlot();
    if (!slot || this.submitting() || this.quantity.invalid || this.notes.invalid) return;
    const max = slot.remainingCapacity ?? 1;
    if (this.quantity.value > max) {
      this.error.set(`Solo quedan ${max} cupos disponibles.`);
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.bookings.create({
      businessId: this.businessId, locationId: this.locationId()!, serviceId: this.serviceId,
      resourceId: slot.resourceId, startsAt: slot.startAt, capacitySessionId: slot.capacitySessionId,
      quantity: slot.capacitySessionId ? this.quantity.value : 1, notes: this.notes.value.trim() || null,
    }, this.idempotencyKey).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (booking) => this.confirmed.set(booking),
      error: (response: HttpErrorResponse) => this.error.set(this.errorMessage(response)),
    });
  }

  dayLabel(day: string): string {
    return new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(`${day}T12:00:00Z`));
  }

  time(value: string): string { return value.slice(11, 16); }
  price(value: number | null, currency = 'PEN'): string {
    return value === null ? 'Precio por confirmar' : new Intl.NumberFormat('es-PE', { style: 'currency', currency }).format(value);
  }

  private errorMessage(error: HttpErrorResponse): string {
    if (error.status === 409) return 'Ese horario acaba de ser reservado. Actualiza la disponibilidad y elige otro.';
    if (error.status === 401) return 'Tu sesión venció. Inicia sesión nuevamente para confirmar.';
    return error.error?.message ?? 'No pudimos confirmar la reserva. Inténtalo nuevamente.';
  }

  private numberParam(value: string | null): number { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0; }
  today(): string { return new Date().toISOString().slice(0, 10); }
  private initialDate(): string { const date = new Date(); date.setDate(date.getDate() + 1); return date.toISOString().slice(0, 10); }
  private addDays(value: string, days: number): string { const date = new Date(`${value}T12:00:00Z`); date.setUTCDate(date.getUTCDate() + days); return date.toISOString().slice(0, 10); }
  private newKey(): string { return globalThis.crypto?.randomUUID?.() ?? `booking-${Date.now()}-${Math.random().toString(16).slice(2)}`; }
}

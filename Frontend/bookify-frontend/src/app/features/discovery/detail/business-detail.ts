import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PublicBusinessDetail } from '../../../core/discovery/discovery.models';
import { DiscoveryService } from '../../../core/discovery/discovery.service';

@Component({
  selector: 'app-business-detail',
  imports: [RouterLink],
  templateUrl: './business-detail.html',
  styleUrl: './business-detail.css',
})
export class BusinessDetail implements OnInit {
  private readonly discovery = inject(DiscoveryService);
  private readonly route = inject(ActivatedRoute);

  readonly business = signal<PublicBusinessDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly selectedLocationId = signal<number | null>(null);
  readonly requestedAt = signal<string | null>(null);
  readonly selectedLocation = computed(() => {
    const business = this.business();
    return business?.locations.find((location) => location.id === this.selectedLocationId()) ?? business?.locations[0] ?? null;
  });
  readonly services = computed(() => {
    const locationId = this.selectedLocation()?.id;
    return this.business()?.services.filter((service) => !locationId || service.locationIds.includes(locationId)) ?? [];
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    this.selectedLocationId.set(this.numberOrNull(this.route.snapshot.queryParamMap.get('location')));
    this.requestedAt.set(this.route.snapshot.queryParamMap.get('at'));
    if (!slug) {
      this.loading.set(false);
      this.error.set('El lugar solicitado no existe.');
      return;
    }
    this.discovery.getBusiness(slug).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (business) => {
        this.business.set(business);
        if (!business.locations.some((location) => location.id === this.selectedLocationId())) {
          this.selectedLocationId.set(business.locations[0]?.id ?? null);
        }
      },
      error: () => this.error.set('No pudimos cargar este negocio. Puede que ya no esté disponible.'),
    });
  }

  chooseLocation(id: number): void {
    this.selectedLocationId.set(id);
  }

  price(value: number | null, currency: string): string {
    if (value === null) return 'Consultar precio';
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency }).format(value);
  }

  dateLabel(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  }

  private numberOrNull(value: string | null): number | null {
    const parsed = Number(value);
    return value !== null && Number.isFinite(parsed) ? parsed : null;
  }
}

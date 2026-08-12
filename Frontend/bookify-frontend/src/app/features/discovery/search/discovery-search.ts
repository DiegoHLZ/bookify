import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { DISCOVERY_CATEGORIES, DiscoverySearchItem } from '../../../core/discovery/discovery.models';
import { DiscoveryService } from '../../../core/discovery/discovery.service';

const LIMA_CENTER = { latitude: -12.0464, longitude: -77.0428 };

@Component({
  selector: 'app-discovery-search',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './discovery-search.html',
  styleUrl: './discovery-search.css',
})
export class DiscoverySearch implements OnInit {
  private readonly discovery = inject(DiscoveryService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly categories = DISCOVERY_CATEGORIES;
  readonly items = signal<DiscoverySearchItem[]>([]);
  readonly loading = signal(false);
  readonly locating = signal(false);
  readonly searched = signal(false);
  readonly error = signal('');
  readonly page = signal(0);
  readonly hasNext = signal(false);
  readonly latitude = signal(LIMA_CENTER.latitude);
  readonly longitude = signal(LIMA_CENTER.longitude);
  readonly locationLabel = signal('Centro de Lima');
  readonly resultSummary = computed(() => {
    const count = this.items().length;
    return count === 1 ? '1 lugar encontrado' : `${count} lugares encontrados`;
  });

  readonly form = new FormGroup({
    text: new FormControl('', { nonNullable: true }),
    categoryCode: new FormControl('', { nonNullable: true }),
    availableAt: new FormControl('', { nonNullable: true }),
    radiusKm: new FormControl(15, { nonNullable: true }),
    minRating: new FormControl(0, { nonNullable: true }),
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.form.patchValue({
      text: params.get('q') ?? '',
      categoryCode: params.get('category') ?? '',
      availableAt: params.get('availableAt') ?? '',
      radiusKm: this.numberParam(params.get('radiusKm'), 15),
      minRating: this.numberParam(params.get('minRating'), 0),
    });
    this.latitude.set(this.numberParam(params.get('lat'), LIMA_CENTER.latitude));
    this.longitude.set(this.numberParam(params.get('lng'), LIMA_CENTER.longitude));
    this.page.set(Math.max(0, this.numberParam(params.get('page'), 0)));
    if (params.has('lat')) this.locationLabel.set('Tu ubicación');
    this.search(this.page(), false);
  }

  submit(): void {
    this.search(0, true);
  }

  previous(): void {
    if (this.page() > 0) this.search(this.page() - 1, true);
  }

  next(): void {
    if (this.hasNext()) this.search(this.page() + 1, true);
  }

  useCurrentLocation(): void {
    if (!navigator.geolocation) {
      this.error.set('Tu navegador no permite obtener la ubicación. Usaremos el centro de Lima.');
      return;
    }
    this.locating.set(true);
    this.error.set('');
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        this.latitude.set(coords.latitude);
        this.longitude.set(coords.longitude);
        this.locationLabel.set('Tu ubicación');
        this.locating.set(false);
        this.search(0, true);
      },
      () => {
        this.locating.set(false);
        this.error.set('No pudimos acceder a tu ubicación. Puedes continuar buscando desde Lima.');
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    );
  }

  distance(meters: number): string {
    return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1000).toFixed(1)} km`;
  }

  categoryName(code: string): string {
    return this.categories.find((category) => category.code === code)?.name ?? code.replaceAll('_', ' ');
  }

  private search(page: number, updateUrl: boolean): void {
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.page.set(page);
    if (updateUrl) {
      void this.router.navigate([], { relativeTo: this.route, queryParams: this.queryParams(page), replaceUrl: true });
    }
    this.discovery.search({
      latitude: this.latitude(), longitude: this.longitude(), radiusKm: value.radiusKm,
      text: value.text, categoryCode: value.categoryCode, minRating: value.minRating,
      availableAt: value.availableAt || undefined, page, size: 12,
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => {
        this.items.set(result.items);
        this.hasNext.set(result.hasNext);
        this.searched.set(true);
      },
      error: () => {
        this.items.set([]);
        this.hasNext.set(false);
        this.searched.set(true);
        this.error.set('No pudimos completar la búsqueda. Inténtalo nuevamente.');
      },
    });
  }

  private queryParams(page: number): Record<string, string | number | null> {
    const value = this.form.getRawValue();
    return {
      q: value.text || null, category: value.categoryCode || null,
      availableAt: value.availableAt || null, radiusKm: value.radiusKm,
      minRating: value.minRating || null, lat: this.latitude(), lng: this.longitude(),
      page: page || null,
    };
  }

  private numberParam(value: string | null, fallback: number): number {
    const parsed = Number(value);
    return value !== null && Number.isFinite(parsed) ? parsed : fallback;
  }
}

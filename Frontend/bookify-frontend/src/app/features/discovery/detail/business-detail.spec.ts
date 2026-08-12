import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { DiscoveryService } from '../../../core/discovery/discovery.service';
import { BusinessDetail } from './business-detail';

describe('BusinessDetail', () => {
  let fixture: ComponentFixture<BusinessDetail>;
  const business = { id: 1, slug: 'estudio-norte', name: 'Estudio Norte', description: null, categoryCode: 'BEAUTY', phone: null, email: null, ratingAverage: 4.7, ratingCount: 8, locations: [{ id: 3, name: 'Principal', address: 'Av. Lima 1', city: 'Lima', countryCode: 'PE', timezone: 'America/Lima', latitude: -12, longitude: -77 }], services: [{ id: 9, name: 'Corte', description: null, durationMinutes: 45, price: 35, currency: 'PEN', bookingMode: 'EXCLUSIVE_RESOURCE' as const, locationIds: [3] }] };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusinessDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'estudio-norte' }, queryParamMap: { get: () => null } } } },
        { provide: DiscoveryService, useValue: { getBusiness: vi.fn().mockReturnValue(of(business)) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BusinessDetail);
    fixture.detectChanges();
  });

  it('shows public services for the selected location', () => {
    expect(fixture.componentInstance.business()?.slug).toBe('estudio-norte');
    expect(fixture.componentInstance.services().map((service) => service.name)).toEqual(['Corte']);
    expect(fixture.nativeElement.textContent).toContain('Estudio Norte');
  });
});

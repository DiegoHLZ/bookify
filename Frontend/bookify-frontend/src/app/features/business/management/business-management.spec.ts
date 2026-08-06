import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BusinessService } from '../../../core/business/business.service';
import { BusinessManagement } from './business-management';

describe('BusinessManagement', () => {
  let component: BusinessManagement;
  let fixture: ComponentFixture<BusinessManagement>;
  const listMine = vi.fn();
  const listLocations = vi.fn();
  const listServices = vi.fn();
  const createService = vi.fn();

  beforeEach(async () => {
    listMine.mockReset().mockReturnValue(of([{ id: 7, name: 'Studio Norte', slug: 'studio-norte', categoryCode: 'BEAUTY_SALON', membershipRole: 'OWNER' }]));
    listLocations.mockReset().mockReturnValue(of([{ id: 3, businessId: 7, name: 'Sede principal', address: 'Av. 123', city: 'Lima', countryCode: 'PE', timezone: 'America/Lima', latitude: -12, longitude: -77, active: true, coordinatesVerified: false, coordinatesVerifiedAt: null, coordinateSource: null, createdAt: '', updatedAt: '' }]));
    listServices.mockReset().mockReturnValue(of([]));
    createService.mockReset();

    await TestBed.configureTestingModule({
      imports: [BusinessManagement],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ businessId: '7' }) } } },
        { provide: BusinessService, useValue: { listMine, listLocations, listServices, createService } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessManagement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('loads the tenant, locations and services from the route id', () => {
    expect(component.business()?.name).toBe('Studio Norte');
    expect(component.activeLocations()).toHaveLength(1);
    expect(component.serviceForm.controls.locationId.value).toBe(3);
    expect(listServices).toHaveBeenCalledWith(7);
  });

  it('does not submit an invalid service', () => {
    component.submit();

    expect(createService).not.toHaveBeenCalled();
    expect(component.serviceForm.controls.name.touched).toBe(true);
  });

  it('creates a service with booking policy defaults', () => {
    createService.mockReturnValue(of({ id: 9, name: 'Corte clásico', description: null, durationMinutes: 45, price: 35, currency: 'PEN', active: true, bookingMode: 'EXCLUSIVE_RESOURCE', customerCancellationAllowed: true, cancellationNoticeMinutes: 120, customerRescheduleAllowed: true, rescheduleNoticeMinutes: 120, maxReschedules: 2, businessId: 7, locationIds: [3], createdAt: '', updatedAt: '' }));
    component.serviceForm.setValue({ name: ' Corte clásico ', description: '', durationMinutes: 45, price: 35, currency: 'PEN', bookingMode: 'EXCLUSIVE_RESOURCE', locationId: 3 });

    component.submit();

    expect(createService).toHaveBeenCalledWith(7, expect.objectContaining({ name: 'Corte clásico', locationIds: [3], cancellationNoticeMinutes: 120 }));
    expect(component.services()).toHaveLength(1);
    expect(component.formOpen()).toBe(false);
  });
});

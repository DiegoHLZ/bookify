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
  const listResources = vi.fn();
  const createService = vi.fn();
  const createResource = vi.fn();
  const changeResourceStatus = vi.fn();
  const getServiceResources = vi.fn();
  const replaceServiceResources = vi.fn();

  beforeEach(async () => {
    listMine.mockReset().mockReturnValue(of([{ id: 7, name: 'Studio Norte', slug: 'studio-norte', categoryCode: 'BEAUTY_SALON', membershipRole: 'OWNER' }]));
    listLocations.mockReset().mockReturnValue(of([{ id: 3, businessId: 7, name: 'Sede principal', address: 'Av. 123', city: 'Lima', countryCode: 'PE', timezone: 'America/Lima', latitude: -12, longitude: -77, active: true, coordinatesVerified: false, coordinatesVerifiedAt: null, coordinateSource: null, createdAt: '', updatedAt: '' }]));
    listServices.mockReset().mockReturnValue(of([]));
    listResources.mockReset().mockReturnValue(of([]));
    createService.mockReset();
    createResource.mockReset();
    changeResourceStatus.mockReset();
    getServiceResources.mockReset();
    replaceServiceResources.mockReset();

    await TestBed.configureTestingModule({
      imports: [BusinessManagement],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ businessId: '7' }) } } },
        { provide: BusinessService, useValue: { listMine, listLocations, listServices, listResources, createService, createResource, changeResourceStatus, getServiceResources, replaceServiceResources } },
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
    expect(listResources).toHaveBeenCalledWith(7, 3);
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

  it('creates a resource in the selected location', () => {
    createResource.mockReturnValue(of({ id: 12, businessId: 7, locationId: 3, name: 'Ana Torres', description: null, type: 'PROFESSIONAL', capacity: 1, active: true, createdAt: '', updatedAt: '' }));
    component.resourceForm.setValue({ name: ' Ana Torres ', description: '', type: 'PROFESSIONAL', capacity: 1, locationId: 3 });

    component.submitResource();

    expect(createResource).toHaveBeenCalledWith(7, 3, { name: 'Ana Torres', description: null, type: 'PROFESSIONAL', capacity: 1 });
    expect(component.resources()).toHaveLength(1);
    expect(component.resourceFormOpen()).toBe(false);
  });

  it('toggles resource status using its tenant and location', () => {
    const resource = { id: 12, businessId: 7, locationId: 3, name: 'Ana Torres', description: null, type: 'PROFESSIONAL' as const, capacity: 1, active: true, createdAt: '', updatedAt: '' };
    component.resources.set([resource]);
    changeResourceStatus.mockReturnValue(of({ ...resource, active: false }));

    component.toggleResource(resource);

    expect(changeResourceStatus).toHaveBeenCalledWith(7, 3, 12, false);
    expect(component.resources()[0]?.active).toBe(false);
  });

  it('loads current assignments and only offers active resources at service locations', () => {
    const service = { id: 9, name: 'Corte clásico', description: null, durationMinutes: 45, price: 35, currency: 'PEN' as const, active: true, bookingMode: 'EXCLUSIVE_RESOURCE' as const, customerCancellationAllowed: true, cancellationNoticeMinutes: 120, customerRescheduleAllowed: true, rescheduleNoticeMinutes: 120, maxReschedules: 2, businessId: 7, locationIds: [3], createdAt: '', updatedAt: '' };
    component.resources.set([
      { id: 12, businessId: 7, locationId: 3, name: 'Ana Torres', description: null, type: 'PROFESSIONAL', capacity: 1, active: true, createdAt: '', updatedAt: '' },
      { id: 13, businessId: 7, locationId: 3, name: 'Inactivo', description: null, type: 'PROFESSIONAL', capacity: 1, active: false, createdAt: '', updatedAt: '' },
      { id: 14, businessId: 7, locationId: 8, name: 'Otra sede', description: null, type: 'ROOM', capacity: 1, active: true, createdAt: '', updatedAt: '' },
    ]);
    getServiceResources.mockReturnValue(of({ serviceId: 9, resourceIds: [12, 13] }));

    component.openAssignment(service);

    expect(getServiceResources).toHaveBeenCalledWith(7, 9);
    expect(component.assignedResourceIds().has(12)).toBe(true);
    expect(component.assignedResourceIds().has(13)).toBe(false);
    expect(component.unavailableAssignmentIds()).toEqual([13]);
    expect(component.eligibleResources().map((resource) => resource.id)).toEqual([12]);
  });

  it('replaces the complete assignment with the selected resource ids', () => {
    const service = { id: 9, name: 'Corte clásico', description: null, durationMinutes: 45, price: 35, currency: 'PEN' as const, active: true, bookingMode: 'EXCLUSIVE_RESOURCE' as const, customerCancellationAllowed: true, cancellationNoticeMinutes: 120, customerRescheduleAllowed: true, rescheduleNoticeMinutes: 120, maxReschedules: 2, businessId: 7, locationIds: [3], createdAt: '', updatedAt: '' };
    component.assignmentService.set(service);
    component.assignedResourceIds.set(new Set([12, 5]));
    replaceServiceResources.mockReturnValue(of({ serviceId: 9, resourceIds: [5, 12] }));

    component.saveAssignment();

    expect(replaceServiceResources).toHaveBeenCalledWith(7, 9, [5, 12]);
    expect(component.assignmentService()).toBeNull();
  });
});

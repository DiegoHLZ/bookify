import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BusinessService } from './business.service';

describe('BusinessService', () => {
  let service: BusinessService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BusinessService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads active memberships', () => {
    service.listMine().subscribe((businesses) => expect(businesses[0]?.name).toBe('Studio Norte'));

    const request = http.expectOne('/api/v1/me/businesses');
    expect(request.request.method).toBe('GET');
    request.flush([{ id: 1, name: 'Studio Norte', slug: 'studio-norte', categoryCode: 'BEAUTY_SALON', membershipRole: 'OWNER' }]);
  });

  it('creates the transactional onboarding payload', () => {
    const payload = {
      name: 'Studio Norte', slug: 'studio-norte', categoryCode: 'BEAUTY_SALON',
      description: null, phone: null, email: null,
      location: { name: 'Sede principal', address: 'Av. Principal 123', city: 'Lima', countryCode: 'PE', timezone: 'America/Lima', latitude: -12.046374, longitude: -77.042793 },
    };
    service.onboard(payload).subscribe();

    const request = http.expectOne('/api/v1/businesses');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ ...payload, id: 1, membershipRole: 'OWNER', location: { ...payload.location, id: 1 } });
  });

  it('loads locations and services for the selected business', () => {
    service.listLocations(7).subscribe();
    service.listServices(7).subscribe();

    const locations = http.expectOne('/api/v1/businesses/7/locations');
    const services = http.expectOne('/api/v1/businesses/7/services');
    expect(locations.request.method).toBe('GET');
    expect(services.request.method).toBe('GET');
    locations.flush([]);
    services.flush([]);
  });

  it('creates a service inside the selected tenant', () => {
    const payload = {
      name: 'Corte clásico', description: null, durationMinutes: 45, price: 35,
      currency: 'PEN' as const, locationIds: [3], bookingMode: 'EXCLUSIVE_RESOURCE' as const,
      customerCancellationAllowed: true, cancellationNoticeMinutes: 120,
      customerRescheduleAllowed: true, rescheduleNoticeMinutes: 120, maxReschedules: 2,
    };
    service.createService(7, payload).subscribe();

    const request = http.expectOne('/api/v1/businesses/7/services');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ ...payload, id: 9, active: true, businessId: 7, createdAt: '', updatedAt: '' });
  });

  it('loads and creates resources inside a business location', () => {
    const payload = { name: 'Ana', description: null, type: 'PROFESSIONAL' as const, capacity: 1 };
    service.listResources(7, 3).subscribe();
    service.createResource(7, 3, payload).subscribe();

    const requests = http.match('/api/v1/businesses/7/locations/3/resources');
    const listRequest = requests.find((request) => request.request.method === 'GET');
    const createRequest = requests.find((request) => request.request.method === 'POST');
    expect(requests).toHaveLength(2);
    expect(listRequest).toBeDefined();
    expect(createRequest).toBeDefined();
    expect(createRequest?.request.body).toEqual(payload);
    listRequest?.flush([]);
    createRequest?.flush({ ...payload, id: 5, businessId: 7, locationId: 3, active: true, createdAt: '', updatedAt: '' });
  });

  it('changes resource availability status', () => {
    service.changeResourceStatus(7, 3, 5, false).subscribe();

    const request = http.expectOne('/api/v1/businesses/7/locations/3/resources/5/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({});
  });
});

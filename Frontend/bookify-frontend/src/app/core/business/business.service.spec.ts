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

  it('loads and atomically replaces service resource assignments', () => {
    service.getServiceResources(7, 9).subscribe();
    service.replaceServiceResources(7, 9, [5, 8]).subscribe();

    const requests = http.match('/api/v1/businesses/7/services/9/resources');
    const getRequest = requests.find((request) => request.request.method === 'GET');
    const putRequest = requests.find((request) => request.request.method === 'PUT');
    expect(requests).toHaveLength(2);
    expect(getRequest).toBeDefined();
    expect(putRequest?.request.body).toEqual({ resourceIds: [5, 8] });
    getRequest?.flush({ serviceId: 9, resourceIds: [5] });
    putRequest?.flush({ serviceId: 9, resourceIds: [5, 8] });
  });

  it('loads and atomically replaces a resource schedule', () => {
    const rules = [{ dayOfWeek: 'MONDAY' as const, ruleType: 'AVAILABLE' as const, startTime: '09:00', endTime: '18:00' }];
    service.getResourceSchedule(7, 3, 5).subscribe();
    service.replaceResourceSchedule(7, 3, 5, rules).subscribe();

    const requests = http.match('/api/v1/businesses/7/locations/3/resources/5/schedule');
    expect(requests.find((request) => request.request.method === 'GET')).toBeDefined();
    expect(requests.find((request) => request.request.method === 'PUT')?.request.body).toEqual({ rules });
    requests.forEach((request) => request.flush({ businessId: 7, locationId: 3, resourceId: 5, timezone: 'America/Lima', rules: [] }));
  });

  it('manages dated schedule exceptions with an explicit range', () => {
    const payload = { exceptionType: 'CLOSED' as const, startTime: null, endTime: null, reason: 'Feriado' };
    service.listScheduleExceptions(7, 3, 5, '2026-08-09', '2027-08-09').subscribe();
    service.upsertScheduleException(7, 3, 5, '2026-12-25', payload).subscribe();
    service.deleteScheduleException(7, 3, 5, '2026-12-25').subscribe();

    const list = http.expectOne((request) => request.url === '/api/v1/businesses/7/locations/3/resources/5/exceptions' && request.params.get('from') === '2026-08-09' && request.params.get('to') === '2027-08-09');
    const dateRequests = http.match('/api/v1/businesses/7/locations/3/resources/5/exceptions/2026-12-25');
    const upsert = dateRequests.find((request) => request.request.method === 'PUT')!;
    const remove = dateRequests.find((request) => request.request.method === 'DELETE')!;
    expect(list.request.method).toBe('GET');
    expect(upsert.request.method).toBe('PUT');
    expect(upsert.request.body).toEqual(payload);
    expect(remove.request.method).toBe('DELETE');
    list.flush([]);
    upsert.flush({ id: 1, resourceId: 5, exceptionDate: '2026-12-25', ...payload, createdAt: '', updatedAt: '' });
    remove.flush(null);
  });
});

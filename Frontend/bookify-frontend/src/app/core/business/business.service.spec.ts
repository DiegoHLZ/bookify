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
});

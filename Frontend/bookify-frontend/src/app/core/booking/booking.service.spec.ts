import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BookingService } from './booking.service';

describe('BookingService', () => {
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a bounded availability window', () => {
    service.availability(2, 3, 4, '2026-08-18', '2026-08-24').subscribe();
    const request = http.expectOne((candidate) => candidate.url.endsWith('/businesses/2/locations/3/services/4/availability'));
    expect(request.request.params.get('from')).toBe('2026-08-18');
    expect(request.request.params.get('to')).toBe('2026-08-24');
    expect(request.request.params.get('intervalMinutes')).toBe('15');
    request.flush({ slots: [] });
  });

  it('creates a booking with its idempotency key', () => {
    const body = { businessId: 2, locationId: 3, serviceId: 4, resourceId: 5, startsAt: '2026-08-18T15:00:00Z', capacitySessionId: null, quantity: 1, notes: null };
    service.create(body, 'booking-key').subscribe();
    const request = http.expectOne('/api/v1/bookings');
    expect(request.request.headers.get('Idempotency-Key')).toBe('booking-key');
    expect(request.request.body).toEqual(body);
    request.flush({ id: 8 });
  });
});

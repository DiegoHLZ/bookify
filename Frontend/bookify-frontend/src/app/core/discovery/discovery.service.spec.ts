import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DiscoveryService } from './discovery.service';

describe('DiscoveryService', () => {
  let service: DiscoveryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DiscoveryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('serializes deterministic search filters', () => {
    service.search({ latitude: -12.1, longitude: -77.03, radiusKm: 15, text: ' masaje ', categoryCode: 'BEAUTY', minRating: 4, availableAt: '2026-08-14T10:00', page: 1, size: 12 }).subscribe();
    const request = http.expectOne((candidate) => candidate.url === '/api/v1/discovery/search');
    expect(request.request.params.get('text')).toBe('masaje');
    expect(request.request.params.get('availableAt')).toBe('2026-08-14T10:00');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({ page: 1, size: 12, hasNext: false, items: [] });
  });

  it('loads a public business by encoded slug', () => {
    service.getBusiness('salón norte').subscribe();
    const request = http.expectOne('/api/v1/discovery/businesses/sal%C3%B3n%20norte');
    request.flush({});
  });
});

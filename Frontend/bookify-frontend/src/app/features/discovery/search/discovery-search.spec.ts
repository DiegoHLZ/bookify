import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DiscoveryService } from '../../../core/discovery/discovery.service';
import { DiscoverySearch } from './discovery-search';

describe('DiscoverySearch', () => {
  let fixture: ComponentFixture<DiscoverySearch>;
  const search = vi.fn().mockReturnValue(of({ page: 0, size: 12, hasNext: false, items: [] }));

  beforeEach(async () => {
    search.mockClear();
    await TestBed.configureTestingModule({
      imports: [DiscoverySearch],
      providers: [provideRouter([]), { provide: DiscoveryService, useValue: { search } }],
    }).compileComponents();
    fixture = TestBed.createComponent(DiscoverySearch);
    fixture.detectChanges();
  });

  it('loads the deterministic marketplace with safe Lima defaults', () => {
    expect(search).toHaveBeenCalledWith(expect.objectContaining({ latitude: -12.0464, longitude: -77.0428, radiusKm: 15, page: 0 }));
    expect(fixture.componentInstance.searched()).toBe(true);
  });

  it('submits filters from the form', () => {
    fixture.componentInstance.form.patchValue({ text: 'cancha', minRating: 4 });
    fixture.componentInstance.submit();
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ text: 'cancha', minRating: 4 }));
  });
});

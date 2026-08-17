import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BookingService } from '../../../core/booking/booking.service';
import { DiscoveryService } from '../../../core/discovery/discovery.service';
import { BookingCreate } from './booking-create';

describe('BookingCreate', () => {
  let fixture: ComponentFixture<BookingCreate>;
  const slot = { resourceId: 5, resourceName: 'Ana', resourceType: 'PROFESSIONAL', capacitySessionId: null, remainingCapacity: null, localStart: '2026-08-20T10:00:00', localEnd: '2026-08-20T10:45:00', startAt: '2026-08-20T15:00:00Z', endAt: '2026-08-20T15:45:00Z' };
  const create = vi.fn().mockReturnValue(of({ id: 8, customerEmail: 'client@example.com' }));
  const availability = vi.fn().mockReturnValue(of({ timezone: 'America/Lima', slots: [slot] }));
  const business = { id: 2, slug: 'estudio', name: 'Estudio', services: [{ id: 4, name: 'Corte', durationMinutes: 45, price: 35, currency: 'PEN', locationIds: [3] }] };

  beforeEach(async () => {
    create.mockClear(); availability.mockClear();
    await TestBed.configureTestingModule({
      imports: [BookingCreate],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (key: string) => ({ businessId: '2', locationId: '3', serviceId: '4', slug: 'estudio' })[key] ?? null } } } },
        { provide: BookingService, useValue: { availability, create } },
        { provide: DiscoveryService, useValue: { getBusiness: vi.fn().mockReturnValue(of(business)) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BookingCreate);
    fixture.detectChanges();
  });

  it('loads slots and confirms the selected booking', () => {
    const component = fixture.componentInstance;
    expect(component.slots()).toEqual([slot]);
    component.select(slot);
    component.notes.setValue('Primera visita');
    component.confirm();
    expect(create).toHaveBeenCalledWith(expect.objectContaining({ resourceId: 5, startsAt: slot.startAt, quantity: 1, notes: 'Primera visita' }), expect.any(String));
    expect(component.confirmed()?.id).toBe(8);
  });

  it('explains an availability conflict', () => {
    create.mockReturnValueOnce(throwError(() => ({ status: 409 })));
    const component = fixture.componentInstance;
    component.select(slot); component.confirm();
    expect(component.error()).toContain('acaba de ser reservado');
  });
});

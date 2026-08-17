import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BusinessService } from '../../../core/business/business.service';
import { Onboarding } from './onboarding';

describe('Onboarding', () => {
  let component: Onboarding;
  let fixture: ComponentFixture<Onboarding>;
  const onboard = vi.fn();

  beforeEach(async () => {
    onboard.mockReset();

    await TestBed.configureTestingModule({
      imports: [Onboarding],
      providers: [
        provideRouter([]),
        { provide: BusinessService, useValue: { onboard } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Onboarding);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('generates a URL-safe slug from the business name', () => {
    component.businessForm.controls.name.setValue('Salón Ámbar & Spa');

    component.updateSlug();

    expect(component.businessForm.controls.slug.value).toBe('salon-ambar-spa');
  });

  it('does not advance while the current step is invalid', () => {
    component.next();

    expect(component.step()).toBe(1);
    expect(component.businessForm.controls.name.touched).toBe(true);
  });

  it('submits the complete transactional onboarding payload', () => {
    onboard.mockReturnValue(of({
      id: 10,
      name: 'Studio Norte',
      slug: 'studio-norte',
      categoryCode: 'BEAUTY_SALON',
      membershipRole: 'OWNER',
      location: {
        id: 20,
        name: 'Sede principal',
        address: 'Av. Principal 123',
        city: 'Lima',
        countryCode: 'PE',
        timezone: 'America/Lima',
        latitude: -12.046374,
        longitude: -77.042793,
      },
    }));
    component.businessForm.setValue({
      name: ' Studio Norte ',
      slug: 'studio-norte',
      categoryCode: 'BEAUTY_SALON',
      description: '',
      phone: '',
      email: '',
    });
    component.locationForm.setValue({
      name: 'Sede principal',
      address: 'Av. Principal 123',
      city: 'Lima',
      countryCode: 'PE',
      timezone: 'America/Lima',
      latitude: -12.046374,
      longitude: -77.042793,
    });

    component.submit();

    expect(onboard).toHaveBeenCalledWith({
      name: 'Studio Norte',
      slug: 'studio-norte',
      categoryCode: 'BEAUTY_SALON',
      description: null,
      phone: null,
      email: null,
      location: {
        name: 'Sede principal',
        address: 'Av. Principal 123',
        city: 'Lima',
        countryCode: 'PE',
        timezone: 'America/Lima',
        latitude: -12.046374,
        longitude: -77.042793,
      },
    });
    expect(component.step()).toBe(4);
    expect(component.createdBusiness()?.id).toBe(10);
  });
});

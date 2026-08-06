export interface BusinessLocationRequest {
  name: string;
  address: string;
  city: string;
  countryCode: string;
  timezone: string;
  latitude: number;
  longitude: number;
}

export interface CreateBusinessRequest {
  name: string;
  slug: string;
  categoryCode: string;
  description: string | null;
  phone: string | null;
  email: string | null;
  location: BusinessLocationRequest;
}

export interface BusinessLocationResponse extends BusinessLocationRequest {
  id: number;
}

export interface BusinessOnboardingResponse {
  id: number;
  name: string;
  slug: string;
  categoryCode: string;
  membershipRole: string;
  location: BusinessLocationResponse;
}

export interface MyBusiness {
  id: number;
  name: string;
  slug: string;
  categoryCode: string;
  membershipRole: string;
}

export type BookingMode = 'EXCLUSIVE_RESOURCE' | 'CAPACITY_SESSION';
export type SupportedCurrency = 'PEN' | 'USD' | 'EUR';

export interface BusinessLocation {
  id: number;
  businessId: number;
  name: string;
  address: string;
  city: string;
  countryCode: string;
  timezone: string;
  latitude: number;
  longitude: number;
  active: boolean;
  coordinatesVerified: boolean;
  coordinatesVerifiedAt: string | null;
  coordinateSource: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceOfferingRequest {
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  currency: SupportedCurrency;
  locationIds: number[];
  bookingMode: BookingMode;
  customerCancellationAllowed: boolean;
  cancellationNoticeMinutes: number;
  customerRescheduleAllowed: boolean;
  rescheduleNoticeMinutes: number;
  maxReschedules: number;
}

export interface ServiceOffering {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  currency: SupportedCurrency;
  active: boolean;
  bookingMode: BookingMode;
  customerCancellationAllowed: boolean;
  cancellationNoticeMinutes: number | null;
  customerRescheduleAllowed: boolean;
  rescheduleNoticeMinutes: number | null;
  maxReschedules: number | null;
  businessId: number;
  locationIds: number[];
  createdAt: string;
  updatedAt: string;
}

export interface BusinessCategoryOption {
  code: string;
  name: string;
  description: string;
}

export const BUSINESS_CATEGORIES: readonly BusinessCategoryOption[] = [
  { code: 'BARBERSHOP', name: 'Barbería', description: 'Cortes, afeitado y cuidado masculino' },
  { code: 'BEAUTY_SALON', name: 'Belleza', description: 'Salones, estética y cuidado personal' },
  { code: 'SPORTS_VENUE', name: 'Deportes', description: 'Canchas, clases y espacios deportivos' },
  { code: 'WELLNESS', name: 'Bienestar', description: 'Masajes, terapias y salud integral' },
  { code: 'COWORKING', name: 'Coworking', description: 'Salas, escritorios y espacios de trabajo' },
  { code: 'PROFESSIONAL_SERVICES', name: 'Servicios profesionales', description: 'Consultoría y atención especializada' },
];

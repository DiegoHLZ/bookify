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

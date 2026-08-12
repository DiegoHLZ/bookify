export interface DiscoverySearchQuery {
  latitude: number;
  longitude: number;
  radiusKm: number;
  text?: string;
  categoryCode?: string;
  minRating?: number;
  availableAt?: string;
  page: number;
  size: number;
}

export interface DiscoverySearchItem {
  businessId: number;
  businessSlug: string;
  businessName: string;
  categoryCode: string;
  ratingAverage: number;
  ratingCount: number;
  locationId: number;
  locationName: string;
  address: string;
  city: string;
  countryCode: string;
  timezone: string;
  latitude: number;
  longitude: number;
  distanceMeters: number;
  requestedAt: string | null;
  availableServiceIds: number[];
}

export interface DiscoverySearchPage {
  page: number;
  size: number;
  hasNext: boolean;
  items: DiscoverySearchItem[];
}

export interface PublicLocation {
  id: number;
  name: string;
  address: string;
  city: string;
  countryCode: string;
  timezone: string;
  latitude: number;
  longitude: number;
}

export interface PublicService {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number | null;
  currency: string;
  bookingMode: 'APPOINTMENT' | 'EXCLUSIVE_RESOURCE' | 'CAPACITY_SESSION';
  locationIds: number[];
}

export interface PublicBusinessDetail {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  categoryCode: string;
  phone: string | null;
  email: string | null;
  ratingAverage: number;
  ratingCount: number;
  locations: PublicLocation[];
  services: PublicService[];
}

export const DISCOVERY_CATEGORIES = [
  { code: '', name: 'Todas las categorías' },
  { code: 'BARBERSHOP', name: 'Barberías' },
  { code: 'BEAUTY_SALON', name: 'Salones de belleza' },
  { code: 'WELLNESS', name: 'Bienestar' },
  { code: 'SPORTS_VENUE', name: 'Instalaciones deportivas' },
  { code: 'COWORKING', name: 'Coworking' },
  { code: 'PROFESSIONAL_SERVICES', name: 'Servicios profesionales' },
] as const;

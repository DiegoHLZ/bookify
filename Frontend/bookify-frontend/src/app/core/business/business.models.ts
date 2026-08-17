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
export type ResourceType = 'PROFESSIONAL' | 'COURT' | 'ROOM' | 'DESK' | 'EQUIPMENT';
export type WeekDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type ScheduleRuleType = 'AVAILABLE' | 'BREAK';
export type ScheduleExceptionType = 'CLOSED' | 'CUSTOM_HOURS';

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

export interface UpsertBookableResourceRequest {
  name: string;
  description: string | null;
  type: ResourceType;
  capacity: number;
}

export interface BookableResource {
  id: number;
  businessId: number;
  locationId: number;
  name: string;
  description: string | null;
  type: ResourceType;
  capacity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceResourceAssignment {
  serviceId: number;
  resourceIds: number[];
}

export interface ScheduleRuleRequest {
  dayOfWeek: WeekDay;
  ruleType: ScheduleRuleType;
  startTime: string;
  endTime: string;
}

export interface ScheduleRule extends ScheduleRuleRequest {
  id: number;
}

export interface ResourceSchedule {
  businessId: number;
  locationId: number;
  resourceId: number;
  timezone: string;
  rules: ScheduleRule[];
}

export interface ScheduleExceptionRequest {
  exceptionType: ScheduleExceptionType;
  startTime: string | null;
  endTime: string | null;
  reason: string | null;
}

export interface ScheduleException extends ScheduleExceptionRequest {
  id: number;
  resourceId: number;
  exceptionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface WeekDayOption {
  code: WeekDay;
  shortName: string;
  name: string;
}

export const WEEK_DAYS: readonly WeekDayOption[] = [
  { code: 'MONDAY', shortName: 'Lun', name: 'Lunes' },
  { code: 'TUESDAY', shortName: 'Mar', name: 'Martes' },
  { code: 'WEDNESDAY', shortName: 'Mié', name: 'Miércoles' },
  { code: 'THURSDAY', shortName: 'Jue', name: 'Jueves' },
  { code: 'FRIDAY', shortName: 'Vie', name: 'Viernes' },
  { code: 'SATURDAY', shortName: 'Sáb', name: 'Sábado' },
  { code: 'SUNDAY', shortName: 'Dom', name: 'Domingo' },
];

export interface ResourceTypeOption {
  code: ResourceType;
  name: string;
  description: string;
}

export const RESOURCE_TYPES: readonly ResourceTypeOption[] = [
  { code: 'PROFESSIONAL', name: 'Profesional', description: 'Persona que presta el servicio' },
  { code: 'COURT', name: 'Cancha', description: 'Espacio deportivo reservable' },
  { code: 'ROOM', name: 'Sala', description: 'Consultorio, cabina o sala' },
  { code: 'DESK', name: 'Escritorio', description: 'Puesto individual de trabajo' },
  { code: 'EQUIPMENT', name: 'Equipo', description: 'Máquina o equipo especializado' },
];

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

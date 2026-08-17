export interface AvailabilitySlot {
  resourceId: number;
  resourceName: string;
  resourceType: string;
  capacitySessionId: number | null;
  remainingCapacity: number | null;
  localStart: string;
  localEnd: string;
  startAt: string;
  endAt: string;
}

export interface ServiceAvailability {
  businessId: number;
  locationId: number;
  serviceId: number;
  durationMinutes: number;
  intervalMinutes: number;
  timezone: string;
  from: string;
  to: string;
  slots: AvailabilitySlot[];
}

export interface CreateBookingRequest {
  businessId: number;
  locationId: number;
  serviceId: number;
  resourceId: number;
  startsAt: string;
  capacitySessionId: number | null;
  quantity: number;
  notes: string | null;
}

export interface Booking {
  id: number;
  businessId: number;
  locationId: number;
  serviceId: number;
  serviceName: string;
  resourceId: number;
  resourceName: string;
  resourceType: string;
  capacitySessionId: number | null;
  customerId: number;
  customerEmail: string;
  startsAt: string;
  endsAt: string;
  localStart: string;
  localEnd: string;
  timezone: string;
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'NO_SHOW' | 'CANCELLED' | 'REJECTED';
  quantity: number;
  notes: string | null;
  createdAt: string;
}

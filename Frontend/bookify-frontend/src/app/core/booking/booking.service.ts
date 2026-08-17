import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Booking, CreateBookingRequest, ServiceAvailability } from './booking.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);

  availability(businessId: number, locationId: number, serviceId: number, from: string, to: string): Observable<ServiceAvailability> {
    const params = new HttpParams().set('from', from).set('to', to).set('intervalMinutes', 15);
    return this.http.get<ServiceAvailability>(
      `/api/v1/businesses/${businessId}/locations/${locationId}/services/${serviceId}/availability`,
      { params },
    );
  }

  create(request: CreateBookingRequest, idempotencyKey: string): Observable<Booking> {
    return this.http.post<Booking>('/api/v1/bookings', request, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    });
  }

  mine(): Observable<Booking[]> {
    return this.http.get<Booking[]>('/api/v1/bookings');
  }
}

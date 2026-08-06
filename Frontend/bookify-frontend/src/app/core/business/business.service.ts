import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BusinessLocation,
  BusinessOnboardingResponse,
  CreateBusinessRequest,
  CreateServiceOfferingRequest,
  MyBusiness,
  ServiceOffering,
} from './business.models';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);

  listMine(): Observable<MyBusiness[]> {
    return this.http.get<MyBusiness[]>('/api/v1/me/businesses');
  }

  onboard(request: CreateBusinessRequest): Observable<BusinessOnboardingResponse> {
    return this.http.post<BusinessOnboardingResponse>('/api/v1/businesses', request);
  }

  listLocations(businessId: number): Observable<BusinessLocation[]> {
    return this.http.get<BusinessLocation[]>(`/api/v1/businesses/${businessId}/locations`);
  }

  listServices(businessId: number): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`/api/v1/businesses/${businessId}/services`);
  }

  createService(businessId: number, request: CreateServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>(`/api/v1/businesses/${businessId}/services`, request);
  }
}

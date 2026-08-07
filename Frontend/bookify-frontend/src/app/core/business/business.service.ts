import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BusinessLocation,
  BookableResource,
  BusinessOnboardingResponse,
  CreateBusinessRequest,
  CreateServiceOfferingRequest,
  MyBusiness,
  ServiceOffering,
  UpsertBookableResourceRequest,
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

  listResources(businessId: number, locationId: number): Observable<BookableResource[]> {
    return this.http.get<BookableResource[]>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources`);
  }

  createResource(businessId: number, locationId: number, request: UpsertBookableResourceRequest): Observable<BookableResource> {
    return this.http.post<BookableResource>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources`, request);
  }

  changeResourceStatus(businessId: number, locationId: number, resourceId: number, active: boolean): Observable<BookableResource> {
    return this.http.patch<BookableResource>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/status`, { active });
  }
}

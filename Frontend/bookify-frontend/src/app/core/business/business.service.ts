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
  ResourceSchedule,
  ScheduleException,
  ScheduleExceptionRequest,
  ScheduleRuleRequest,
  ServiceResourceAssignment,
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

  getServiceResources(businessId: number, serviceId: number): Observable<ServiceResourceAssignment> {
    return this.http.get<ServiceResourceAssignment>(`/api/v1/businesses/${businessId}/services/${serviceId}/resources`);
  }

  replaceServiceResources(businessId: number, serviceId: number, resourceIds: number[]): Observable<ServiceResourceAssignment> {
    return this.http.put<ServiceResourceAssignment>(`/api/v1/businesses/${businessId}/services/${serviceId}/resources`, { resourceIds });
  }

  getResourceSchedule(businessId: number, locationId: number, resourceId: number): Observable<ResourceSchedule> {
    return this.http.get<ResourceSchedule>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/schedule`);
  }

  replaceResourceSchedule(businessId: number, locationId: number, resourceId: number, rules: ScheduleRuleRequest[]): Observable<ResourceSchedule> {
    return this.http.put<ResourceSchedule>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/schedule`, { rules });
  }

  listScheduleExceptions(businessId: number, locationId: number, resourceId: number, from: string, to: string): Observable<ScheduleException[]> {
    return this.http.get<ScheduleException[]>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/exceptions`, { params: { from, to } });
  }

  upsertScheduleException(businessId: number, locationId: number, resourceId: number, date: string, request: ScheduleExceptionRequest): Observable<ScheduleException> {
    return this.http.put<ScheduleException>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/exceptions/${date}`, request);
  }

  deleteScheduleException(businessId: number, locationId: number, resourceId: number, date: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/businesses/${businessId}/locations/${locationId}/resources/${resourceId}/exceptions/${date}`);
  }
}

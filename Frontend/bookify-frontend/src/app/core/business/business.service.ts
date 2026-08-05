import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BusinessOnboardingResponse, CreateBusinessRequest, MyBusiness } from './business.models';

@Injectable({ providedIn: 'root' })
export class BusinessService {
  private readonly http = inject(HttpClient);

  listMine(): Observable<MyBusiness[]> {
    return this.http.get<MyBusiness[]>('/api/v1/me/businesses');
  }

  onboard(request: CreateBusinessRequest): Observable<BusinessOnboardingResponse> {
    return this.http.post<BusinessOnboardingResponse>('/api/v1/businesses', request);
  }
}

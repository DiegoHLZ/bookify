import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DiscoverySearchPage, DiscoverySearchQuery, PublicBusinessDetail } from './discovery.models';

@Injectable({ providedIn: 'root' })
export class DiscoveryService {
  private readonly http = inject(HttpClient);

  search(query: DiscoverySearchQuery): Observable<DiscoverySearchPage> {
    let params = new HttpParams()
      .set('latitude', query.latitude)
      .set('longitude', query.longitude)
      .set('radiusKm', query.radiusKm)
      .set('page', query.page)
      .set('size', query.size);

    if (query.text?.trim()) params = params.set('text', query.text.trim());
    if (query.categoryCode) params = params.set('categoryCode', query.categoryCode);
    if (query.minRating) params = params.set('minRating', query.minRating);
    if (query.availableAt) params = params.set('availableAt', query.availableAt);

    return this.http.get<DiscoverySearchPage>('/api/v1/discovery/search', { params });
  }

  getBusiness(slug: string): Observable<PublicBusinessDetail> {
    return this.http.get<PublicBusinessDetail>(`/api/v1/discovery/businesses/${encodeURIComponent(slug)}`);
  }
}

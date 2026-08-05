import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { BUSINESS_CATEGORIES, MyBusiness } from '../../core/business/business.models';
import { BusinessService } from '../../core/business/business.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly businessService = inject(BusinessService);
  private readonly router = inject(Router);

  readonly businesses = signal<MyBusiness[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);

  constructor() {
    this.loadBusinesses();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/']);
  }

  loadBusinesses(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.businessService.listMine().subscribe({
      next: (businesses) => {
        this.businesses.set(businesses);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  categoryName(code: string): string {
    return BUSINESS_CATEGORIES.find((category) => category.code === code)?.name
      ?? code.toLowerCase().replace(/_/g, ' ').replace(/^./, (value) => value.toUpperCase());
  }
}

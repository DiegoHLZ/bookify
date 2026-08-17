import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly router = inject(Router);
  readonly categories = [
    { icon: '✦', name: 'Belleza', detail: 'Salones y especialistas', code: 'BEAUTY_SALON' },
    { icon: '●', name: 'Bienestar', detail: 'Terapias y cuidado personal', code: 'WELLNESS' },
    { icon: '▲', name: 'Deportes', detail: 'Canchas y entrenadores', code: 'SPORTS_VENUE' },
    { icon: '■', name: 'Espacios', detail: 'Salas y coworking', code: 'COWORKING' },
  ];

  explore(query: string): void {
    void this.router.navigate(['/explorar'], { queryParams: { q: query.trim() || null } });
  }
}

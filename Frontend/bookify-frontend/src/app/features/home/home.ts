import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  readonly categories = [
    { icon: '✦', name: 'Belleza', detail: 'Salones y especialistas' },
    { icon: '●', name: 'Bienestar', detail: 'Terapias y cuidado personal' },
    { icon: '▲', name: 'Deportes', detail: 'Canchas y entrenadores' },
    { icon: '■', name: 'Espacios', detail: 'Salas y coworking' },
  ];
}

import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((module) => module.Home),
    title: 'Bookify | Reserva servicios cerca de ti',
  },
  {
    path: 'iniciar-sesion',
    loadComponent: () => import('./features/auth/login/login').then((module) => module.Login),
    title: 'Iniciar sesión | Bookify',
  },
  {
    path: 'crear-cuenta',
    loadComponent: () => import('./features/auth/register/register').then((module) => module.Register),
    title: 'Crear cuenta | Bookify',
  },
  {
    path: 'panel',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then((module) => module.Dashboard),
    title: 'Mi panel | Bookify',
  },
  {
    path: 'negocio/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./features/business/onboarding/onboarding').then((module) => module.Onboarding),
    title: 'Configura tu negocio | Bookify',
  },
  {
    path: 'negocios/:businessId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/business/management/business-management').then((module) => module.BusinessManagement),
    title: 'Gestiona tu negocio | Bookify',
  },
  { path: '**', redirectTo: '' },
];

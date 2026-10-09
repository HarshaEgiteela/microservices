import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then(m => m.HomeComponent)
  },
  { path: 'login', loadComponent: () => import('./features/auth/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register').then(m => m.RegisterComponent) },
  { path: 'products', redirectTo: '' },
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () => import('./features/products/cart').then(m => m.CartComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders').then(m => m.OrdersComponent)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin').then(m => m.AdminComponent)
  },
  { path: '**', redirectTo: '' }
];

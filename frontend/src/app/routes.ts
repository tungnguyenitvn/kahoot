import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { Auth } from './api';
const hostGuard: CanActivateFn = async () => {
  const auth = inject(Auth); const router = inject(Router);
  try { await auth.ready(); return auth.user()?.host ? true : router.parseUrl('/login'); }
  catch { return router.parseUrl('/login'); }
};
export const routes: Routes = [
  { path: '', loadComponent: () => import('./entry').then(m => m.Entry) },
  { path: 'login', loadComponent: () => import('./login').then(m => m.Login) },
  { path: 'host', canActivate: [hostGuard], loadComponent: () => import('./studio').then(m => m.Studio) },
  { path: 'room/:id', loadComponent: () => import('./room').then(m => m.RoomPage) },
  { path: '**', redirectTo: '' }
];

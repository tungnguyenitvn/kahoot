import { Routes } from '@angular/router';
import { hostGuard } from './core/host.guard';
/** Routes are the only composition point between features. */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/entry/entry.page').then(m => m.Entry) },
  { path: 'login', loadComponent: () => import('./features/login/login.page').then(m => m.Login) },
  { path: 'host', canActivate: [hostGuard], loadComponent: () => import('./features/studio/studio.page').then(m => m.Studio) },
  { path: 'room/:id', loadComponent: () => import('./features/room/room.page').then(m => m.RoomPage) },
  { path: '**', redirectTo: '' }
];

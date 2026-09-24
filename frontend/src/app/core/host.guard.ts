import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';
/** Host-only routes; the server re-checks authority on every request, this only routes. */
export const hostGuard: CanActivateFn = async () => {
  const auth = inject(Auth); const router = inject(Router);
  try { await auth.ready(); return auth.user()?.host ? true : router.parseUrl('/login'); }
  catch { return router.parseUrl('/login'); }
};

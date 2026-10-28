
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.hasToken()) {
    return router.createUrlTree(['/login']);
  }

  return auth.loadProfile().pipe(
    map(profile => {
      return profile.role === 'ADMIN'
        ? true
        : router.createUrlTree(['/products']);
    }),

    catchError(() => {
      return of(router.createUrlTree(['/login']));
    })
  );
};

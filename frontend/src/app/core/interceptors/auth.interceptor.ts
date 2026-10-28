
import { inject } from '@angular/core';
import {
  HttpInterceptorFn,
  HttpErrorResponse
} from '@angular/common/http';

import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.getToken();

  // Attach JWT to authenticated API requests.
  const authReq = token
    ? req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
    : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {

      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        token &&
        auth.getToken() === token &&
        !req.url.includes('/users/login')
      ) {
        // Clear invalid or expired authentication.
        auth.logout();

        // Redirect to login.
        router.navigate(['/login']);
      }

      // Preserve the original error for the caller.
      return throwError(() => error);
    })
  );
};

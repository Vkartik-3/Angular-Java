import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  return from(authService.ensureFreshToken()).pipe(
    switchMap(() => {
      const token = authService.token;
      return next(
        token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req
      );
    })
  );
};

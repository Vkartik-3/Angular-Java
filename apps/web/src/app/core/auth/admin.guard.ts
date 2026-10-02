import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuthenticated) {
    auth.login();
    return false;
  }
  if (auth.hasRole('admin')) return true;
  return inject(Router).createUrlTree(['/orders']);
};
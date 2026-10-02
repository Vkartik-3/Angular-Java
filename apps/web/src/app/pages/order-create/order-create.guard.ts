import { CanDeactivateFn } from '@angular/router';

export interface CanComponentDeactivate {
  canDeactivate(): boolean;
}

export const unsavedCartGuard: CanDeactivateFn<CanComponentDeactivate> = (component) =>
  component.canDeactivate();
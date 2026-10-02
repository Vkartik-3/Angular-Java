import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
  // Intentionally left on default change detection: this component reads
  // auth.username / auth.hasRole(), plain getters over Keycloak's internal
  // (non-signal) state. Under OnPush those could go stale without a
  // navbar-local event to force a re-check.
})
export class NavbarComponent {
  auth = inject(AuthService);
  cart = inject(CartService);

  initial(): string {
    return (this.auth.username ?? '?').charAt(0).toUpperCase();
  }
}

import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, ButtonModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
  // Intentionally left on default change detection: this component reads
  // auth.username / auth.hasRole(), plain getters over Keycloak's internal
  // (non-signal) state. Under OnPush those could go stale without a
  // navbar-local event to force a re-check.
})
export class NavbarComponent {
  auth = inject(AuthService);
}

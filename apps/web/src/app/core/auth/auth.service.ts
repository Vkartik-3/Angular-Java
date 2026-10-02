import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private kc = new Keycloak({
    url: environment.keycloakUrl,
    realm: 'shopflow',
    clientId: 'shopflow-app'
  });

  init(): Promise<boolean> {
    return this.kc.init({ onLoad: 'login-required', checkLoginIframe: false, redirectUri: window.location.origin + '/orders' });
  }

  get token(): string | undefined {
    return this.kc.token;
  }

  async ensureFreshToken(): Promise<void> {
    try {
      await this.kc.updateToken(30);
    } catch {
      // refresh failed — proceed with current token
    }
  }

  get username(): string | undefined {
    return this.kc.tokenParsed?.['preferred_username'] as string | undefined;
  }

  get isAuthenticated(): boolean {
    return !!this.kc.authenticated;
  }

  hasRole(role: string): boolean {
    return this.kc.hasRealmRole(role);
  }

  login(): void {
    void this.kc.login();
  }

  logout(): void {
    void this.kc.logout();
  }
}

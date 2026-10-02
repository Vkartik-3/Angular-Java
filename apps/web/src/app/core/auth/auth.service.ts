import { Injectable } from '@angular/core';
import type Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';
import { clearDemoState } from '../demo/demo-storage';

const DEMO_USER = 'demo';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private kc?: Keycloak;

  async init(): Promise<boolean> {
    if (environment.demo) return true;
    // Loaded lazily: keeps keycloak-js out of the initial bundle and out of the demo build.
    const { default: KeycloakClient } = await import('keycloak-js');
    this.kc = new KeycloakClient({
      url: environment.keycloakUrl,
      realm: 'groove',
      clientId: 'groove-app'
    });
    return this.kc.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      redirectUri: window.location.origin + '/orders'
    });
  }

  get isDemo(): boolean {
    return environment.demo;
  }

  get token(): string | undefined {
    return this.kc?.token;
  }

  async ensureFreshToken(): Promise<void> {
    if (!this.kc) return;
    try {
      await this.kc.updateToken(30);
    } catch {
      // refresh failed — proceed with current token
    }
  }

  get username(): string | undefined {
    if (environment.demo) return DEMO_USER;
    return this.kc?.tokenParsed?.['preferred_username'] as string | undefined;
  }

  get isAuthenticated(): boolean {
    return environment.demo || !!this.kc?.authenticated;
  }

  hasRole(role: string): boolean {
    // The demo user holds both realm roles so every screen is reachable.
    if (environment.demo) return role === 'user' || role === 'admin';
    return !!this.kc?.hasRealmRole(role);
  }

  login(): void {
    void this.kc?.login();
  }

  logout(): void {
    if (environment.demo) {
      clearDemoState();
      window.location.reload();
      return;
    }
    void this.kc?.logout();
  }
}

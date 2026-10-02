import { HttpInterceptorFn } from '@angular/common/http';

export interface AppEnvironment {
  /** Runs the SPA against an in-browser simulated backend (GitHub Pages build). */
  demo: boolean;
  /** Keycloak base URL used for the OIDC login redirect. Ignored in demo mode. */
  keycloakUrl: string;
  /** Grafana base URL for the per-order log link; null hides the link. */
  grafanaUrl: string | null;
  /** Source repository, linked from the footer and the demo banner. */
  repoUrl: string;
  /** Extra HTTP interceptors appended after the auth interceptor. */
  httpInterceptors: HttpInterceptorFn[];
}

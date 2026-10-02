import { AppEnvironment } from './environment.model';
import { demoBackendInterceptor } from '../app/core/demo/demo-backend.interceptor';

export const environment: AppEnvironment = {
  demo: true,
  keycloakUrl: '',
  grafanaUrl: null,
  repoUrl: 'https://github.com/Vkartik-3/Angular-Java',
  httpInterceptors: [demoBackendInterceptor]
};

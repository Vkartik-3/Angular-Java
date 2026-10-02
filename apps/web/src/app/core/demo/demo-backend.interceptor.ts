import { HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { defer, delay, from, Observable } from 'rxjs';
import { DemoBackend } from './demo-backend';

/** Simulated network latency so loading states behave as they would against the gateway. */
const LATENCY_MS = 180;

let backend: DemoBackend | undefined;

export const demoBackendInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) return next(req);
  backend ??= new DemoBackend();
  const instance = backend;
  return defer(() => from(instance.handle(req))).pipe(delay(LATENCY_MS)) as Observable<HttpEvent<unknown>>;
};

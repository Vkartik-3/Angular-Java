# Groove web app

Angular 21 SPA for Groove. See the [root README](../../README.md) for the full system.

## Scripts

| Command | What it does |
|---|---|
| `npm start` | Dev server against the real gateway (proxies `/api`, see `proxy.conf.json`) |
| `npm run start:demo` | Dev server with the in-browser simulated backend, no services needed |
| `npm run build` | Production build into `dist/frontend` |
| `npm run build:demo` | Demo build (the one published to GitHub Pages) |
| `npm test -- --watch=false` | Unit tests (Vitest) |
| `npm run e2e` | Playwright suite against the full Docker Compose stack |

## Build configurations

- **production / development** use `environment.ts` / `environment.prod.ts`: Keycloak login, real API, SSE order updates.
- **demo** replaces the environment with `environment.demo.ts`, which registers `demoBackendInterceptor`. Every `/api/*` request is answered by `core/demo/demo-backend.ts`, a faithful model of the gateway contract and the saga state machine. State persists in `localStorage`.

## Structure

```
src/app/
  core/
    auth/        Keycloak wrapper (loaded lazily), guards, token interceptor
    demo/        Simulated backend for the demo build
    models/      API types, saga helpers (liveness, pipeline projection)
    services/    HTTP clients, cart state (signals)
  pages/         orders, shop (order-create), order detail, admin
  shared/        navbar
  theme.ts       PrimeNG preset
styles.scss      Design tokens and shared layout primitives
```

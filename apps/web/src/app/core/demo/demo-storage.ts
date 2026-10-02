// Kept separate from demo-backend.ts so AuthService can reset the demo
// without pulling the simulated backend into the production bundle.
export const DEMO_STORAGE_KEY = 'groove-demo-v1';

export function demoStorage(): Storage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

export function clearDemoState(): void {
  try {
    demoStorage()?.removeItem(DEMO_STORAGE_KEY);
  } catch {
    // storage blocked — nothing persisted, nothing to clear
  }
}

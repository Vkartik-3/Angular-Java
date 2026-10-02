import { HistoryStatus, StatusHistoryEntry } from './order.model';

const TERMINAL = new Set<HistoryStatus>(['INVENTORY_APPROVED', 'PAYMENT_FAILED', 'PAYMENT_ROLLED_BACK']);

/**
 * True while the saga can still emit status transitions. A CANCELLED entry is
 * only final if payment was never charged; otherwise a PAYMENT_ROLLED_BACK
 * compensation is still on its way.
 */
export function isSagaLive(history: StatusHistoryEntry[] | null | undefined): boolean {
  if (!history?.length) return true;
  const last = history[history.length - 1].status;
  if (TERMINAL.has(last)) return false;
  if (last === 'CANCELLED') {
    return history.some(h => h.status === 'PAID');
  }
  return true;
}

export type StepState = 'done' | 'active' | 'failed' | 'compensated' | 'pending' | 'skipped';

export interface SagaStep {
  key: 'order' | 'payment' | 'inventory';
  label: string;
  service: string;
  state: StepState;
  caption: string;
}

/** Projects the status history onto the three saga participants for the pipeline view. */
export function sagaSteps(history: StatusHistoryEntry[] | null | undefined): SagaStep[] {
  const seen = new Set((history ?? []).map(h => h.status));
  const live = isSagaLive(history);
  const paid = seen.has('PAID');
  const cancelled = seen.has('CANCELLED');

  let payment: Pick<SagaStep, 'state' | 'caption'>;
  if (seen.has('PAYMENT_FAILED')) payment = { state: 'failed', caption: 'Declined' };
  else if (paid && seen.has('PAYMENT_ROLLED_BACK')) payment = { state: 'compensated', caption: 'Refunded' };
  else if (paid && (seen.has('INVENTORY_REJECTED') || cancelled)) payment = { state: 'active', caption: 'Rolling back' };
  else if (paid) payment = { state: 'done', caption: 'Charged' };
  else if (cancelled) payment = { state: 'failed', caption: 'Cancelled' };
  else payment = live ? { state: 'active', caption: 'Processing' } : { state: 'pending', caption: 'Waiting' };

  let inventory: Pick<SagaStep, 'state' | 'caption'>;
  if (seen.has('INVENTORY_APPROVED')) inventory = { state: 'done', caption: 'Reserved' };
  else if (seen.has('INVENTORY_REJECTED')) inventory = { state: 'failed', caption: 'Rejected' };
  else if (paid && cancelled) inventory = { state: 'failed', caption: 'Cancelled' };
  else if (!paid && (cancelled || seen.has('PAYMENT_FAILED'))) inventory = { state: 'skipped', caption: 'Skipped' };
  else if (paid && live) inventory = { state: 'active', caption: 'Checking stock' };
  else inventory = { state: 'pending', caption: 'Waiting' };

  return [
    { key: 'order', label: 'Order', service: 'order-service', state: 'done', caption: 'Saga started' },
    { key: 'payment', label: 'Payment', service: 'payment-service', ...payment },
    { key: 'inventory', label: 'Inventory', service: 'inventory-service', ...inventory }
  ];
}

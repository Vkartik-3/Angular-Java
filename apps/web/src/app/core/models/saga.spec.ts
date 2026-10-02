import { HistoryStatus } from './order.model';
import { isSagaLive, sagaSteps } from './saga';

const h = (...statuses: HistoryStatus[]) => statuses.map(status => ({ status, occurredAt: '2026-01-01T00:00:00Z' }));
const captions = (...statuses: HistoryStatus[]) => sagaSteps(h(...statuses)).map(s => `${s.state}:${s.caption}`);

describe('isSagaLive', () => {
  it('is live until a terminal status arrives', () => {
    expect(isSagaLive([])).toBe(true);
    expect(isSagaLive(h('CREATED', 'PAID'))).toBe(true);
    expect(isSagaLive(h('CREATED', 'PAID', 'INVENTORY_APPROVED'))).toBe(false);
    expect(isSagaLive(h('CREATED', 'PAYMENT_FAILED'))).toBe(false);
  });

  it('treats CANCELLED as final only when nothing was charged', () => {
    expect(isSagaLive(h('CREATED', 'CANCELLED'))).toBe(false);
    expect(isSagaLive(h('CREATED', 'PAID', 'CANCELLED'))).toBe(true);
    expect(isSagaLive(h('CREATED', 'PAID', 'CANCELLED', 'PAYMENT_ROLLED_BACK'))).toBe(false);
  });
});

describe('sagaSteps', () => {
  it('shows progress while in flight', () => {
    expect(captions('CREATED')).toEqual(['done:Saga started', 'active:Processing', 'pending:Waiting']);
    expect(captions('CREATED', 'PAID')).toEqual(['done:Saga started', 'done:Charged', 'active:Checking stock']);
  });

  it('marks a completed saga', () => {
    expect(captions('CREATED', 'PAID', 'INVENTORY_APPROVED')).toEqual(['done:Saga started', 'done:Charged', 'done:Reserved']);
  });

  it('shows payment failure and skips inventory', () => {
    expect(captions('CREATED', 'PAYMENT_FAILED')).toEqual(['done:Saga started', 'failed:Declined', 'skipped:Skipped']);
  });

  it('shows compensation in progress and completed', () => {
    expect(captions('CREATED', 'PAID', 'INVENTORY_REJECTED'))
      .toEqual(['done:Saga started', 'active:Rolling back', 'failed:Rejected']);
    expect(captions('CREATED', 'PAID', 'INVENTORY_REJECTED', 'PAYMENT_ROLLED_BACK'))
      .toEqual(['done:Saga started', 'compensated:Refunded', 'failed:Rejected']);
  });
});

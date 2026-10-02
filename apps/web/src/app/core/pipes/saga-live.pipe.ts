import { Pipe, PipeTransform } from '@angular/core';
import { StatusHistoryEntry } from '../models/order.model';

const TERMINAL = new Set(['INVENTORY_APPROVED', 'PAYMENT_FAILED', 'PAYMENT_ROLLED_BACK']);

@Pipe({ name: 'sagaLive', standalone: true, pure: true })
export class SagaLivePipe implements PipeTransform {
  transform(history: StatusHistoryEntry[] | null | undefined): boolean {
    if (!history?.length) return true;
    const last = history[history.length - 1].status;
    if (TERMINAL.has(last)) return false;
    if (last === 'CANCELLED') {
      return history.some(h => h.status === 'PAID');
    }
    return true;
  }
}

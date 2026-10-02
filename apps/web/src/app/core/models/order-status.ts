import { OrderStatus } from './order.model';

export type TagSeverity = 'success' | 'info' | 'danger' | 'secondary';
export type StatusGroup = 'progress' | 'completed' | 'failed' | 'cancelled';

export function statusSeverity(status: OrderStatus): TagSeverity {
  switch (status) {
    case OrderStatus.INVENTORY_APPROVED: return 'success';
    case OrderStatus.PAYMENT_FAILED:
    case OrderStatus.INVENTORY_REJECTED: return 'danger';
    case OrderStatus.CANCELLED:          return 'secondary';
    default:                             return 'info';
  }
}

export function statusGroup(status: OrderStatus): StatusGroup {
  switch (status) {
    case OrderStatus.INVENTORY_APPROVED: return 'completed';
    case OrderStatus.PAYMENT_FAILED:
    case OrderStatus.INVENTORY_REJECTED: return 'failed';
    case OrderStatus.CANCELLED:          return 'cancelled';
    default:                             return 'progress';
  }
}

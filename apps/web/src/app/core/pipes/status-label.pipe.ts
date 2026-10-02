import { Pipe, PipeTransform } from '@angular/core';

const LABELS: Record<string, string> = {
  CREATED:             'Order Created',
  PAID:                'Payment Confirmed',
  INVENTORY_APPROVED:  'Inventory Reserved',
  PAYMENT_FAILED:      'Payment Failed',
  INVENTORY_REJECTED:  'Inventory Rejected',
  CANCELLED:           'Order Cancelled',
  PAYMENT_ROLLED_BACK: 'Payment Rolled Back',
};

@Pipe({ name: 'statusLabel', standalone: true, pure: true })
export class StatusLabelPipe implements PipeTransform {
  transform(status: string | null | undefined): string {
    return status ? (LABELS[status] ?? status) : '';
  }
}
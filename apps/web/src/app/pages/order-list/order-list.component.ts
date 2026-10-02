import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe, DecimalPipe, SlicePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { OrderService } from '../../core/services/order.service';
import { OrderItemResponse, OrderResponse, OrderStatus } from '../../core/models/order.model';
import { StatusLabelPipe } from '../../core/pipes/status-label.pipe';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [TableModule, ButtonModule, TagModule, TooltipModule, ToolbarModule, DecimalPipe, DatePipe, SlicePipe, StatusLabelPipe],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private router = inject(Router);

  orders = signal<OrderResponse[]>([]);
  loading = signal(true);

  ngOnInit() {
    this.orderService.getAll().subscribe({
      next: orders => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getSeverity(status: OrderStatus): 'success' | 'info' | 'danger' | 'secondary' {
    switch (status) {
      case OrderStatus.INVENTORY_APPROVED:  return 'success';
      case OrderStatus.PAYMENT_FAILED:
      case OrderStatus.INVENTORY_REJECTED:  return 'danger';
      case OrderStatus.CANCELLED:           return 'secondary';
      default:                              return 'info';
    }
  }

  totalItems(items: OrderItemResponse[]): number {
    return items.reduce((sum, item) => sum + item.quantity, 0);
  }

  view(id: string) {
    void this.router.navigate(['/orders', id]);
  }

  newOrder() {
    void this.router.navigate(['/orders/new']);
  }
}

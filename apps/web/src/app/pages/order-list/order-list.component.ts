import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe, DatePipe, SlicePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/auth/auth.service';
import { OrderItemResponse, OrderResponse, OrderStatus } from '../../core/models/order.model';
import { StatusGroup, statusGroup, statusSeverity, TagSeverity } from '../../core/models/order-status';
import { StatusLabelPipe } from '../../core/pipes/status-label.pipe';

type Filter = 'all' | StatusGroup;

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [TableModule, ButtonModule, TagModule, TooltipModule, CurrencyPipe, DatePipe, SlicePipe, StatusLabelPipe],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private router = inject(Router);
  private messageService = inject(MessageService);
  protected auth = inject(AuthService);

  orders = signal<OrderResponse[]>([]);
  loading = signal(true);
  filter = signal<Filter>('all');
  query = signal('');

  readonly filters: { id: Filter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'progress', label: 'In progress' },
    { id: 'completed', label: 'Completed' },
    { id: 'failed', label: 'Failed' },
    { id: 'cancelled', label: 'Cancelled' }
  ];

  readonly counts = computed(() => {
    const counts: Record<Filter, number> = { all: 0, progress: 0, completed: 0, failed: 0, cancelled: 0 };
    for (const o of this.orders()) {
      counts.all++;
      counts[statusGroup(o.status)]++;
    }
    return counts;
  });

  readonly revenue = computed(() =>
    this.orders()
      .filter(o => o.status === OrderStatus.INVENTORY_APPROVED)
      .reduce((sum, o) => sum + o.total, 0)
  );

  readonly successRate = computed(() => {
    const { completed, failed, cancelled } = this.counts();
    const finished = completed + failed + cancelled;
    return finished ? Math.round((completed / finished) * 100) : null;
  });

  readonly visible = computed(() => {
    const f = this.filter();
    const q = this.query().trim().toLowerCase();
    return this.orders().filter(o =>
      (f === 'all' || statusGroup(o.status) === f) &&
      (!q || o.id.toLowerCase().includes(q) || o.username?.toLowerCase().includes(q) ||
        o.items.some(i => i.productName?.toLowerCase().includes(q)))
    );
  });

  ngOnInit() {
    this.orderService.getAll().subscribe({
      next: orders => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({ severity: 'error', summary: 'Could not load orders', detail: 'The order service is unreachable.' });
      }
    });
  }

  getSeverity(status: OrderStatus): TagSeverity {
    return statusSeverity(status);
  }

  totalItems(items: OrderItemResponse[]): number {
    return items.reduce((sum, item) => sum + item.quantity, 0);
  }

  covers(items: OrderItemResponse[]): OrderItemResponse[] {
    return items.filter(i => i.imageUrl).slice(0, 3);
  }

  onSearch(event: Event) {
    this.query.set((event.target as HTMLInputElement).value);
  }

  view(id: string) {
    void this.router.navigate(['/orders', id]);
  }

  newOrder() {
    void this.router.navigate(['/orders/new']);
  }
}

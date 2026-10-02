import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe, DatePipe, NgOptimizedImage } from '@angular/common';
import { Subscription } from 'rxjs';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { OrderService } from '../../core/services/order.service';
import { HistoryStatus, OrderResponse, OrderStatus } from '../../core/models/order.model';
import { SagaLivePipe } from '../../core/pipes/saga-live.pipe';
import { StatusLabelPipe } from '../../core/pipes/status-label.pipe';

interface TimelineEntry {
  status: string;
  label: string;
  icon: string;
  color: string;
  occurredAt: Date;
}

const STATUS_CONFIG: Record<HistoryStatus, { label: string; icon: string; color: string }> = {
  CREATED:              { label: 'Order Created',       icon: 'pi pi-shopping-cart', color: '#3b82f6' },
  PAID:                 { label: 'Payment Confirmed',   icon: 'pi pi-credit-card',   color: '#3b82f6' },
  INVENTORY_APPROVED:   { label: 'Inventory Reserved',  icon: 'pi pi-box',           color: '#22c55e' },
  PAYMENT_FAILED:       { label: 'Payment Failed',      icon: 'pi pi-times',         color: '#ef4444' },
  INVENTORY_REJECTED:   { label: 'Inventory Rejected',  icon: 'pi pi-ban',           color: '#ef4444' },
  CANCELLED:            { label: 'Order Cancelled',     icon: 'pi pi-times-circle',  color: '#94a3b8' },
  PAYMENT_ROLLED_BACK:  { label: 'Payment Rolled Back', icon: 'pi pi-refresh',       color: '#f59e0b' },
};

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [TableModule, TagModule, TimelineModule, DecimalPipe, DatePipe, NgOptimizedImage, SagaLivePipe, StatusLabelPipe],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  order = signal<OrderResponse | null>(null);
  loading = signal(true);
  isWatching = signal(false);
  corrId = signal<string | undefined>(undefined);

  grafanaUrl = computed(() => {
    const id = this.corrId();
    if (!id) return null;
    return `http://localhost:3000/d/shopflow-logs?var-corrId=${encodeURIComponent(id)}&from=now-1h&to=now`;
  });

  private sub?: Subscription;

  timelineEntries = computed<TimelineEntry[]>(() => {
    const history = this.order()?.history ?? [];
    return history.map(h => {
      const cfg = STATUS_CONFIG[h.status] ?? { label: h.status, icon: 'pi pi-circle', color: '#94a3b8' };
      return { status: h.status, ...cfg, occurredAt: new Date(h.occurredAt) };
    });
  });

  ngOnInit() {
    this.corrId.set(history.state?.corrId);
    const id = this.route.snapshot.paramMap.get('id')!;
    this.sub = this.orderService.watchOrder(id).subscribe({
      next: order => {
        this.order.set(order);
        this.loading.set(false);
        this.isWatching.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.isWatching.set(false);
      },
      complete: () => this.isWatching.set(false)
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  getSeverity(status: OrderStatus): 'success' | 'info' | 'danger' | 'secondary' {
    switch (status) {
      case OrderStatus.INVENTORY_APPROVED:  return 'success';
      case OrderStatus.CREATED:
      case OrderStatus.PAID:                return 'info';
      case OrderStatus.PAYMENT_FAILED:
      case OrderStatus.INVENTORY_REJECTED:  return 'danger';
      case OrderStatus.CANCELLED:           return 'secondary';
    }
  }
}

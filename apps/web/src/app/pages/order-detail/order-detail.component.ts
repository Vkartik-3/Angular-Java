import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { OrderService } from '../../core/services/order.service';
import { HistoryStatus, OrderResponse, OrderStatus } from '../../core/models/order.model';
import { statusSeverity, TagSeverity } from '../../core/models/order-status';
import { sagaSteps } from '../../core/models/saga';
import { SagaLivePipe } from '../../core/pipes/saga-live.pipe';
import { StatusLabelPipe } from '../../core/pipes/status-label.pipe';
import { environment } from '../../../environments/environment';

interface TimelineEntry {
  status: string;
  label: string;
  icon: string;
  tone: 'info' | 'success' | 'danger' | 'warn' | 'neutral';
  occurredAt: Date;
  offset: string | null;
}

const STATUS_CONFIG: Record<HistoryStatus, { label: string; icon: string; tone: TimelineEntry['tone'] }> = {
  CREATED:             { label: 'Order Created',       icon: 'pi pi-shopping-cart', tone: 'info' },
  PAID:                { label: 'Payment Confirmed',   icon: 'pi pi-credit-card',   tone: 'info' },
  INVENTORY_APPROVED:  { label: 'Inventory Reserved',  icon: 'pi pi-box',           tone: 'success' },
  PAYMENT_FAILED:      { label: 'Payment Failed',      icon: 'pi pi-times',         tone: 'danger' },
  INVENTORY_REJECTED:  { label: 'Inventory Rejected',  icon: 'pi pi-ban',           tone: 'danger' },
  CANCELLED:           { label: 'Order Cancelled',     icon: 'pi pi-times-circle',  tone: 'neutral' },
  PAYMENT_ROLLED_BACK: { label: 'Payment Rolled Back', icon: 'pi pi-replay',        tone: 'warn' },
};

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [TagModule, TooltipModule, CurrencyPipe, DatePipe, RouterLink, SagaLivePipe, StatusLabelPipe],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private messageService = inject(MessageService);

  order = signal<OrderResponse | null>(null);
  loading = signal(true);
  isWatching = signal(false);
  corrId = signal<string | undefined>(undefined);

  grafanaUrl = computed(() => {
    const id = this.corrId();
    if (!id || !environment.grafanaUrl) return null;
    return `${environment.grafanaUrl}/d/groove-logs?var-corrId=${encodeURIComponent(id)}&from=now-1h&to=now`;
  });

  steps = computed(() => sagaSteps(this.order()?.history));

  itemCount = computed(() => (this.order()?.items ?? []).reduce((sum, i) => sum + i.quantity, 0));

  private sub?: Subscription;

  timelineEntries = computed<TimelineEntry[]>(() => {
    const history = this.order()?.history ?? [];
    const start = history.length ? new Date(history[0].occurredAt).getTime() : 0;
    return history.map((h, i) => {
      const cfg = STATUS_CONFIG[h.status] ?? { label: h.status, icon: 'pi pi-circle', tone: 'neutral' as const };
      const at = new Date(h.occurredAt);
      const offset = i === 0 ? null : `+${((at.getTime() - start) / 1000).toFixed(1)}s`;
      return { status: h.status, ...cfg, occurredAt: at, offset };
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

  getSeverity(status: OrderStatus): TagSeverity {
    return statusSeverity(status);
  }

  copyId(id: string) {
    navigator.clipboard?.writeText(id).then(
      () => this.messageService.add({ severity: 'success', summary: 'Order ID copied', life: 1500 }),
      () => {}
    );
  }
}

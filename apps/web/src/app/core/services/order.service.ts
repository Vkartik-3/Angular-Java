import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateOrderRequest, OrderResponse, OrderStatus } from '../models/order.model';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  getAll(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>('/api/orders');
  }

  getById(id: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/${id}`);
  }

  create(request: CreateOrderRequest): Observable<HttpResponse<void>> {
    const idempotencyKey = crypto.randomUUID();
    return this.http.post<void>('/api/orders', request, {
      observe: 'response',
      headers: { 'Idempotency-Key': idempotencyKey }
    });
  }

  cancel(id: string): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`/api/orders/${id}/cancel`, null);
  }

  watchOrder(id: string): Observable<OrderResponse> {
    const TERMINAL = new Set<string>([
      OrderStatus.INVENTORY_APPROVED, OrderStatus.PAYMENT_FAILED, 'PAYMENT_ROLLED_BACK'
    ]);

    return new Observable<OrderResponse>(subscriber => {
      let active = true;
      let streamClosed = false;
      const abort = new AbortController();

      const fetchOrder = () => {
        if (!active) return;
        this.http.get<OrderResponse>(`/api/orders/${id}`).subscribe({
          next: order => {
            if (!active) return;
            subscriber.next(order);
            const last = order.history?.at(-1)?.status;
            if (last && (TERMINAL.has(last) || streamClosed)) {
              cleanup();
              subscriber.complete();
            }
          },
          error: err => { cleanup(); subscriber.error(err); }
        });
      };

      const cleanup = () => {
        active = false;
        abort.abort();
      };

      fetchOrder();

      const token = this.auth.token;
      fetch(`/api/orders/${id}/events`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        signal: abort.signal
      }).then(async response => {
        const reader = response.body!.getReader();
        const decoder = new TextDecoder();
        while (active) {
          const { done, value } = await reader.read();
          if (done) break;
          if (decoder.decode(value, { stream: true }).includes('data:')) fetchOrder();
        }
        // Stream closed by server — saga is done, fetch final state
        streamClosed = true;
        if (active) fetchOrder();
      }).catch(() => {
        // Connection lost — fetch current state so we don't hang forever
        streamClosed = true;
        if (active) fetchOrder();
      });

      return cleanup;
    });
  }
}
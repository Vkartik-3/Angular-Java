import { HttpErrorResponse, HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { HistoryStatus, OrderItemResponse, OrderResponse, OrderStatus } from '../models/order.model';
import { ImportResult, Product, SkippedRecord } from '../models/product.model';
import { isSagaLive } from '../models/saga';
import { DEMO_CATALOG } from './demo-catalog';
import { DEMO_STORAGE_KEY, demoStorage } from './demo-storage';

/** Outbox poll + Kafka round trip for one saga hop. */
export const DEMO_HOP_MS = 1_200;
/** Saga step deadline. The backend default is 30 s; shortened so the demo stays watchable. */
export const DEMO_STEP_TIMEOUT_MS = 15_000;

const DEMO_USER = 'demo';
const REQUIRED_ATTRIBUTES: Record<string, string[]> = {
  vinyl: ['artist', 'title'],
  turntable: ['manufacturer', 'name']
};
const EXPECTED_HEADERS = [
  'category,artist,title,price,imageUrl',
  'category,manufacturer,name,price,imageUrl'
];

export interface DemoSettings {
  paymentAccept: boolean;
  inventoryAccept: boolean;
  paymentDelay: number;
  inventoryDelay: number;
  paymentCrash: boolean;
  inventoryCrash: boolean;
}

interface TimelineEntry {
  status: HistoryStatus;
  at: number;
}

interface StoredOrder {
  id: string;
  username: string;
  items: OrderItemResponse[];
  total: number;
  createdAt: number;
  timeline: TimelineEntry[];
  idempotencyKey?: string;
}

interface DemoState {
  products: Product[];
  orders: StoredOrder[];
  settings: DemoSettings;
}

export const DEFAULT_SETTINGS: DemoSettings = {
  paymentAccept: true,
  inventoryAccept: true,
  paymentDelay: 0,
  inventoryDelay: 0,
  paymentCrash: false,
  inventoryCrash: false
};

/**
 * Plans every status transition the orchestrator would record for an order
 * placed at `start`, mirroring OrderSagaOrchestrator and SagaTimeoutJob.
 * Transitions are revealed as wall-clock time passes, so a reload mid-saga
 * picks up exactly where it left off.
 */
export function planSaga(start: number, s: DemoSettings): TimelineEntry[] {
  const timeline: TimelineEntry[] = [{ status: 'CREATED', at: start }];
  const paymentHop = DEMO_HOP_MS + s.paymentDelay * 1000;

  // Payment consumer crashes → message goes to DLQ, saga times out in WAITING_PAYMENT.
  if (s.paymentCrash) {
    timeline.push({ status: 'CANCELLED', at: start + DEMO_STEP_TIMEOUT_MS });
    return timeline;
  }

  let t = start + paymentHop;
  if (!s.paymentAccept) {
    timeline.push({ status: 'PAYMENT_FAILED', at: t });
    return timeline;
  }
  timeline.push({ status: 'PAID', at: t });

  // Inventory consumer crashes → timeout in WAITING_INVENTORY, cancel + payment rollback.
  if (s.inventoryCrash) {
    const cancelledAt = t + DEMO_STEP_TIMEOUT_MS;
    timeline.push({ status: 'CANCELLED', at: cancelledAt });
    timeline.push({ status: 'PAYMENT_ROLLED_BACK', at: cancelledAt + paymentHop });
    return timeline;
  }

  t += DEMO_HOP_MS + s.inventoryDelay * 1000;
  if (!s.inventoryAccept) {
    timeline.push({ status: 'INVENTORY_REJECTED', at: t });
    timeline.push({ status: 'PAYMENT_ROLLED_BACK', at: t + paymentHop });
    return timeline;
  }
  timeline.push({ status: 'INVENTORY_APPROVED', at: t });
  return timeline;
}

/** In-browser stand-in for the API gateway, used by the GitHub Pages build. */
export class DemoBackend {
  private state: DemoState;

  constructor(
    private readonly storage: Storage | null = demoStorage(),
    private readonly now: () => number = Date.now
  ) {
    this.state = this.load();
  }

  async handle(req: HttpRequest<unknown>): Promise<HttpResponse<unknown>> {
    const url = new URL(req.urlWithParams, 'http://demo.local');
    const path = url.pathname.replace(/\/+$/, '');
    const params = url.searchParams;
    const m = req.method;

    if (path === '/api/products' && m === 'GET') return this.ok(this.state.products);
    if (path === '/api/products/import' && m === 'POST') return this.ok(await this.importProducts(req.body));

    if (path === '/api/orders' && m === 'GET') {
      const orders = [...this.state.orders].sort((a, b) => b.createdAt - a.createdAt);
      return this.ok(orders.map(o => this.toResponse(o)));
    }
    if (path === '/api/orders' && m === 'POST') return this.createOrder(req);

    const orderMatch = path.match(/^\/api\/orders\/([^/]+)(\/cancel)?$/);
    if (orderMatch) {
      const order = this.state.orders.find(o => o.id === orderMatch[1]);
      if (!order) throw this.error(404, `Order ${orderMatch[1]} not found`);
      if (!orderMatch[2] && m === 'GET') return this.ok(this.toResponse(order));
      if (orderMatch[2] && m === 'PUT') return this.ok(this.cancel(order));
    }

    const controlMatch = path.match(/^\/api\/(payment|inventory)\/(mode|delay|crash)$/);
    if (controlMatch) return this.control(controlMatch[1] as 'payment' | 'inventory', controlMatch[2], m, params);

    throw this.error(404, `No demo handler for ${m} ${path}`);
  }

  private createOrder(req: HttpRequest<unknown>): HttpResponse<unknown> {
    const body = req.body as { items?: Partial<OrderItemResponse>[] } | null;
    const items = body?.items ?? [];
    if (!items.length) throw this.error(400, 'items must not be empty');
    for (const i of items) {
      if (!i.productId) throw this.error(400, 'productId is required');
      if (!Number.isInteger(i.quantity) || (i.quantity ?? 0) < 1) throw this.error(400, 'quantity must be at least 1');
      if (typeof i.price !== 'number' || i.price <= 0) throw this.error(400, 'price must be positive');
    }

    const idempotencyKey = req.headers.get('Idempotency-Key') ?? undefined;
    const existing = idempotencyKey && this.state.orders.find(o => o.idempotencyKey === idempotencyKey);
    const correlationId = crypto.randomUUID();
    if (existing) return this.accepted(existing.id, correlationId);

    const now = this.now();
    const orderItems = items.map(i => ({
      productId: i.productId!,
      quantity: i.quantity!,
      price: i.price!,
      productName: i.productName ?? null,
      imageUrl: i.imageUrl ?? null
    }));
    const order: StoredOrder = {
      id: crypto.randomUUID(),
      username: DEMO_USER,
      items: orderItems,
      total: round2(orderItems.reduce((sum, i) => sum + i.price * i.quantity, 0)),
      createdAt: now,
      timeline: planSaga(now, this.state.settings),
      idempotencyKey
    };
    this.state.orders.push(order);
    this.save();
    return this.accepted(order.id, correlationId);
  }

  /** Mirrors OrderSagaOrchestrator.cancelByUser. */
  private cancel(order: StoredOrder): OrderResponse {
    const now = this.now();
    const visible = order.timeline.filter(e => e.at <= now);
    const last = visible.at(-1)?.status;
    if (last === 'CREATED') {
      order.timeline = [...visible, { status: 'CANCELLED', at: now }];
    } else if (last === 'PAID') {
      const rollbackAt = now + DEMO_HOP_MS + this.state.settings.paymentDelay * 1000;
      order.timeline = [...visible, { status: 'CANCELLED', at: now }, { status: 'PAYMENT_ROLLED_BACK', at: rollbackAt }];
    }
    this.save();
    return this.toResponse(order);
  }

  private control(service: 'payment' | 'inventory', setting: string, method: string, params: URLSearchParams) {
    const s = this.state.settings;
    const key = {
      mode: service === 'payment' ? 'paymentAccept' : 'inventoryAccept',
      delay: service === 'payment' ? 'paymentDelay' : 'inventoryDelay',
      crash: service === 'payment' ? 'paymentCrash' : 'inventoryCrash'
    }[setting] as keyof DemoSettings;

    if (method === 'GET') return this.ok(s[key]);
    if (method !== 'PUT') throw this.error(405, 'Method not allowed');

    if (setting === 'delay') {
      const seconds = Number(params.get('seconds'));
      if (!Number.isInteger(seconds) || seconds < 0 || seconds > 60) throw this.error(400, 'seconds must be 0-60');
      (s[key] as number) = seconds;
    } else {
      const raw = params.get(setting === 'mode' ? 'accept' : 'enabled');
      if (raw !== 'true' && raw !== 'false') throw this.error(400, 'expected true or false');
      (s[key] as boolean) = raw === 'true';
    }
    this.save();
    return this.ok('OK');
  }

  /** Mirrors ProductImportService + ProductItemProcessor validation. */
  private async importProducts(body: unknown): Promise<ImportResult> {
    const file = body instanceof FormData ? body.get('file') : null;
    if (!(file instanceof Blob) || !(file as File).name?.toLowerCase().endsWith('.csv')) {
      throw this.error(400, 'Invalid file type — only .csv files are accepted.');
    }
    const lines = (await file.text()).split(/\r?\n/);
    const header = (lines[0] ?? '').trim();
    if (!EXPECTED_HEADERS.includes(header)) {
      throw this.error(400, `Invalid CSV header. Expected one of: ${EXPECTED_HEADERS.join(' | ')}, got: ${header}`);
    }
    const columns = header.split(',');
    const products: Product[] = [];
    const skippedRecords: SkippedRecord[] = [];

    lines.slice(1).forEach((line, idx) => {
      if (!line.trim()) return;
      const identifier = `Line ${idx + 2}`;
      const values = line.split(',');
      if (values.length !== columns.length) {
        skippedRecords.push({ identifier, reason: `Wrong number of columns. Required ${columns.length}, found ${values.length}` });
        return;
      }
      const row = Object.fromEntries(columns.map((c, i) => [c, values[i].trim()]));
      try {
        products.push(this.toProduct(row, products.length));
      } catch (e) {
        skippedRecords.push({ identifier, reason: (e as Error).message });
      }
    });

    this.state.products = products;
    this.save();
    return { imported: products.length, skipped: skippedRecords.length, skippedRecords };
  }

  private toProduct(row: Record<string, string>, index: number): Product {
    const category = row['category'] ?? '';
    const required = REQUIRED_ATTRIBUTES[category];
    if (!required) throw new Error(`Unknown category: "${category}"`);
    const attributes: Record<string, string> = {};
    for (const key of required) {
      if (!row[key]) throw new Error(`${key[0].toUpperCase()}${key.slice(1)} is required`);
      attributes[key] = row[key];
    }
    if (!row['price']) throw new Error('Price is required');
    const price = Number(row['price']);
    if (!Number.isFinite(price)) throw new Error(`Invalid price: "${row['price']}"`);
    // Assets resolve against <base href>, which is a sub-path on GitHub Pages.
    const imageUrl = (row['imageUrl'] ?? '').replace(/^\/+/, '');
    return { id: `import-${this.now()}-${index}`, category, price, imageUrl, attributes };
  }

  private toResponse(o: StoredOrder): OrderResponse {
    const now = this.now();
    const visible = o.timeline.filter(e => e.at <= now);
    const history = visible.map(e => ({ status: e.status, occurredAt: new Date(e.at).toISOString() }));
    // PAYMENT_ROLLED_BACK is history-only; the order keeps its previous status.
    const statusEntry = [...visible].reverse().find(e => e.status !== 'PAYMENT_ROLLED_BACK');
    return {
      id: o.id,
      username: o.username,
      status: (statusEntry?.status ?? OrderStatus.CREATED) as OrderStatus,
      items: o.items,
      total: o.total,
      history,
      createdAt: new Date(o.createdAt).toISOString()
    };
  }

  /** Exposed for tests. */
  isLive(orderId: string): boolean {
    const order = this.state.orders.find(o => o.id === orderId);
    return !!order && isSagaLive(this.toResponse(order).history);
  }

  private accepted(id: string, correlationId: string): HttpResponse<unknown> {
    return new HttpResponse({
      status: 202,
      statusText: 'Accepted',
      body: null,
      headers: new HttpHeaders({ Location: `/api/orders/${id}`, 'X-Correlation-ID': correlationId })
    });
  }

  private ok(body: unknown): HttpResponse<unknown> {
    return new HttpResponse({ status: 200, statusText: 'OK', body });
  }

  private error(status: number, message: string): HttpErrorResponse {
    return new HttpErrorResponse({ status, statusText: message, error: { error: message } });
  }

  private load(): DemoState {
    try {
      const raw = this.storage?.getItem(DEMO_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as DemoState;
        if (Array.isArray(parsed.products) && Array.isArray(parsed.orders) && parsed.settings) {
          return { ...parsed, settings: { ...DEFAULT_SETTINGS, ...parsed.settings } };
        }
      }
    } catch {
      // corrupt or unreadable state — fall through to a fresh seed
    }
    return this.seed();
  }

  private save(): void {
    try {
      this.storage?.setItem(DEMO_STORAGE_KEY, JSON.stringify(this.state));
    } catch {
      // storage full or blocked — the demo keeps working in memory
    }
  }

  /** One historical order per saga outcome, like the backend's V14 seed migration. */
  private seed(): DemoState {
    const now = this.now();
    const hour = 3_600_000;
    const byTitle = (title: string) => DEMO_CATALOG.find(p => p.attributes['title'] === title || p.attributes['name'] === title)!;
    const item = (title: string, quantity: number): OrderItemResponse => {
      const p = byTitle(title);
      const name = p.category === 'vinyl'
        ? `${p.attributes['artist']} — ${p.attributes['title']}`
        : `${p.attributes['manufacturer']} — ${p.attributes['name']}`;
      return { productId: p.id, quantity, price: p.price, productName: name, imageUrl: p.imageUrl };
    };
    const order = (id: string, ageHours: number, items: OrderItemResponse[], outcome: Partial<DemoSettings>): StoredOrder => {
      const createdAt = now - ageHours * hour;
      return {
        id, username: DEMO_USER, items, createdAt,
        total: round2(items.reduce((s, i) => s + i.price * i.quantity, 0)),
        timeline: planSaga(createdAt, { ...DEFAULT_SETTINGS, ...outcome })
      };
    };

    return {
      products: DEMO_CATALOG.map(p => ({ ...p, attributes: { ...p.attributes } })),
      settings: { ...DEFAULT_SETTINGS },
      orders: [
        order('e3f2a1b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b', 96, [item('IV', 1), item('Animals', 1)], {}),
        order('9c8d7e6f-5a4b-4c3d-2e1f-0a9b8c7d6e5f', 53, [item('Vol. 4', 1)], { inventoryAccept: false }),
        order('4b1e9d2c-7a3f-4c8e-9b6d-1f2a3c4d5e6f', 30, [item('Planar 6', 1)], { paymentAccept: false }),
        order('2f4d6b8a-0e2c-4a4e-8f0b-2c4e6a8c0e2a', 7, [item('Abbey Road', 2), item('Jailbreak', 1)], {})
      ]
    };
  }
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

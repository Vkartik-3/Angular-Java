import { HttpErrorResponse, HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { OrderResponse } from '../models/order.model';
import { ImportResult, Product } from '../models/product.model';
import { DEFAULT_SETTINGS, DEMO_HOP_MS, DEMO_STEP_TIMEOUT_MS, DemoBackend, planSaga } from './demo-backend';

class MemoryStorage implements Storage {
  private data = new Map<string, string>();
  get length() { return this.data.size; }
  clear() { this.data.clear(); }
  getItem(k: string) { return this.data.get(k) ?? null; }
  key(i: number) { return [...this.data.keys()][i] ?? null; }
  removeItem(k: string) { this.data.delete(k); }
  setItem(k: string, v: string) { this.data.set(k, v); }
}

const statuses = (s: Partial<typeof DEFAULT_SETTINGS>) =>
  planSaga(0, { ...DEFAULT_SETTINGS, ...s }).map(e => e.status);

describe('planSaga', () => {
  it('happy path reserves inventory after payment', () => {
    expect(statuses({})).toEqual(['CREATED', 'PAID', 'INVENTORY_APPROVED']);
  });

  it('payment rejection ends the saga without compensation', () => {
    expect(statuses({ paymentAccept: false })).toEqual(['CREATED', 'PAYMENT_FAILED']);
  });

  it('inventory rejection compensates the charged payment', () => {
    expect(statuses({ inventoryAccept: false })).toEqual(['CREATED', 'PAID', 'INVENTORY_REJECTED', 'PAYMENT_ROLLED_BACK']);
  });

  it('payment consumer crash times the saga out in WAITING_PAYMENT', () => {
    const plan = planSaga(0, { ...DEFAULT_SETTINGS, paymentCrash: true });
    expect(plan.map(e => e.status)).toEqual(['CREATED', 'CANCELLED']);
    expect(plan[1].at).toBe(DEMO_STEP_TIMEOUT_MS);
  });

  it('inventory consumer crash times out and rolls payment back', () => {
    expect(statuses({ inventoryCrash: true })).toEqual(['CREATED', 'PAID', 'CANCELLED', 'PAYMENT_ROLLED_BACK']);
  });

  it('applies the configured step delays', () => {
    const plan = planSaga(0, { ...DEFAULT_SETTINGS, paymentDelay: 2, inventoryDelay: 4 });
    expect(plan[1].at).toBe(DEMO_HOP_MS + 2000);
    expect(plan[2].at).toBe(2 * DEMO_HOP_MS + 6000);
  });
});

describe('DemoBackend', () => {
  let clock: number;
  let backend: DemoBackend;
  const call = (method: 'GET' | 'PUT' | 'POST', url: string, body: unknown = null, headers?: HttpHeaders) =>
    backend.handle(new HttpRequest(method, url, body, { headers })) as Promise<HttpResponse<any>>;
  const order = { items: [{ productId: 'p1', quantity: 2, price: 10.5, productName: 'A — B', imageUrl: 'x.jpg' }] };

  beforeEach(() => {
    clock = Date.UTC(2026, 0, 1);
    backend = new DemoBackend(new MemoryStorage(), () => clock);
  });

  it('seeds a catalogue and one order per saga outcome', async () => {
    const products = (await call('GET', '/api/products')).body as Product[];
    const orders = (await call('GET', '/api/orders')).body as OrderResponse[];
    expect(products.length).toBe(40);
    expect(orders.map(o => o.status).sort()).toEqual(
      ['INVENTORY_APPROVED', 'INVENTORY_APPROVED', 'INVENTORY_REJECTED', 'PAYMENT_FAILED']
    );
  });

  it('creates an order with 202, Location and correlation id, then reveals transitions over time', async () => {
    const res = await call('POST', '/api/orders', order);
    expect(res.status).toBe(202);
    const id = res.headers.get('Location')!.split('/').pop()!;
    expect(res.headers.get('X-Correlation-ID')).toBeTruthy();

    let detail = (await call('GET', `/api/orders/${id}`)).body as OrderResponse;
    expect(detail.status).toBe('CREATED');
    expect(detail.total).toBe(21);
    expect(backend.isLive(id)).toBe(true);

    clock += 10 * DEMO_HOP_MS;
    detail = (await call('GET', `/api/orders/${id}`)).body as OrderResponse;
    expect(detail.history.map(h => h.status)).toEqual(['CREATED', 'PAID', 'INVENTORY_APPROVED']);
    expect(backend.isLive(id)).toBe(false);
  });

  it('keeps INVENTORY_REJECTED as order status after the rollback is recorded', async () => {
    await call('PUT', '/api/inventory/mode?accept=false');
    const id = (await call('POST', '/api/orders', order)).headers.get('Location')!.split('/').pop()!;
    clock += 10 * DEMO_HOP_MS;
    const detail = (await call('GET', `/api/orders/${id}`)).body as OrderResponse;
    expect(detail.status).toBe('INVENTORY_REJECTED');
    expect(detail.history.at(-1)!.status).toBe('PAYMENT_ROLLED_BACK');
  });

  it('returns the original order for a repeated Idempotency-Key', async () => {
    const headers = new HttpHeaders({ 'Idempotency-Key': 'key-1' });
    const first = await call('POST', '/api/orders', order, headers);
    const second = await call('POST', '/api/orders', order, headers);
    expect(second.headers.get('Location')).toBe(first.headers.get('Location'));
    expect(((await call('GET', '/api/orders')).body as OrderResponse[]).length).toBe(5);
  });

  it('rejects an empty order with 400', async () => {
    await expect(call('POST', '/api/orders', { items: [] })).rejects.toBeInstanceOf(HttpErrorResponse);
  });

  it('returns 404 for an unknown order', async () => {
    await expect(call('GET', '/api/orders/nope')).rejects.toMatchObject({ status: 404 });
  });

  it('persists admin settings across instances', async () => {
    const storage = new MemoryStorage();
    backend = new DemoBackend(storage, () => clock);
    await call('PUT', '/api/payment/delay?seconds=4');
    backend = new DemoBackend(storage, () => clock);
    expect((await call('GET', '/api/payment/delay')).body).toBe(4);
  });

  it('cancelling while waiting for inventory triggers a payment rollback', async () => {
    await call('PUT', '/api/inventory/delay?seconds=8');
    const id = (await call('POST', '/api/orders', order)).headers.get('Location')!.split('/').pop()!;
    clock += DEMO_HOP_MS + 100;
    await call('PUT', `/api/orders/${id}/cancel`);
    clock += 30_000;
    const detail = (await call('GET', `/api/orders/${id}`)).body as OrderResponse;
    expect(detail.history.map(h => h.status)).toEqual(['CREATED', 'PAID', 'CANCELLED', 'PAYMENT_ROLLED_BACK']);
    expect(detail.status).toBe('CANCELLED');
  });

  describe('CSV import', () => {
    const upload = (name: string, content: string) => {
      const form = new FormData();
      form.append('file', new File([content], name, { type: 'text/csv' }));
      return call('POST', '/api/products/import', form);
    };

    it('imports valid rows, skips invalid ones and replaces the catalogue', async () => {
      const csv = [
        'category,artist,title,price,imageUrl',
        'vinyl,Pink Floyd,Animals,36.99,/assets/products/pf-animals.jpg',
        'vinyl,,No Artist,10,/x.jpg',
        'vinyl,A,B,abc,/x.jpg',
        'vinyl,too,many,columns,1,2'
      ].join('\n');
      const result = (await upload('mix.csv', csv)).body as ImportResult;
      expect(result.imported).toBe(1);
      expect(result.skippedRecords).toEqual([
        { identifier: 'Line 3', reason: 'Artist is required' },
        { identifier: 'Line 4', reason: 'Invalid price: "abc"' },
        { identifier: 'Line 5', reason: 'Wrong number of columns. Required 5, found 6' }
      ]);
      const products = (await call('GET', '/api/products')).body as Product[];
      expect(products.length).toBe(1);
      expect(products[0].imageUrl).toBe('assets/products/pf-animals.jpg');
    });

    it('rejects a non-CSV file and an unknown header', async () => {
      await expect(upload('data.txt', 'x')).rejects.toMatchObject({ status: 400 });
      await expect(upload('data.csv', 'a,b,c\n1,2,3')).rejects.toMatchObject({ status: 400 });
    });
  });
});

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { OrderService } from '../../core/services/order.service';
import { CartService } from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';
import { ProductDisplayNamePipe } from '../../core/pipes/product-display-name.pipe';
import { CanComponentDeactivate } from './order-create.guard';

type Category = 'all' | 'vinyl' | 'turntable';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [ButtonModule, CurrencyPipe, RouterLink, ProductDisplayNamePipe],
  templateUrl: './order-create.component.html',
  styleUrl: './order-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderCreateComponent implements CanComponentDeactivate {
  private orderService = inject(OrderService);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private productService = inject(ProductService);

  protected cartService = inject(CartService);

  readonly products = toSignal(
    this.productService.getAll().pipe(catchError(() => {
      this.messageService.add({ severity: 'error', summary: 'Could not load the catalogue' });
      return of([] as Product[]);
    })),
    { initialValue: null }
  );
  readonly submitting = signal(false);
  readonly category = signal<Category>('all');
  readonly query = signal('');
  readonly skeletons = Array.from({ length: 8 });

  readonly categories = computed(() => {
    const all = this.products() ?? [];
    return [
      { id: 'all' as Category, label: 'All', count: all.length },
      { id: 'vinyl' as Category, label: 'Vinyl', count: all.filter(p => p.category === 'vinyl').length },
      { id: 'turntable' as Category, label: 'Turntables', count: all.filter(p => p.category === 'turntable').length }
    ];
  });

  readonly visible = computed(() => {
    const c = this.category();
    const q = this.query().trim().toLowerCase();
    return (this.products() ?? []).filter(p =>
      (c === 'all' || p.category === c) &&
      (!q || Object.values(p.attributes).some(v => v.toLowerCase().includes(q)))
    );
  });

  readonly quantities = computed(() =>
    new Map(this.cartService.cart().map(i => [i.product.id, i.quantity]))
  );

  title(p: Product): string {
    return p.category === 'vinyl' ? p.attributes['title'] : p.attributes['name'];
  }

  subtitle(p: Product): string {
    return p.category === 'vinyl' ? p.attributes['artist'] : p.attributes['manufacturer'];
  }

  onSearch(event: Event) {
    this.query.set((event.target as HTMLInputElement).value);
  }

  submit(): void {
    if (this.submitting()) return;
    const nameOf = new ProductDisplayNamePipe();
    const items = this.cartService.cart().map(i => ({
      productId: i.product.id,
      quantity: i.quantity,
      price: i.product.price,
      productName: nameOf.transform(i.product),
      imageUrl: i.product.imageUrl
    }));
    if (!items.length) return;

    this.submitting.set(true);
    this.orderService.create({ items }).subscribe({
      next: response => {
        const orderId = response.headers.get('Location')?.split('/').pop();
        const corrId = response.headers.get('X-Correlation-ID') ?? undefined;
        if (orderId) {
          this.cartService.clear();
          void this.router.navigate(['/orders', orderId], { state: { corrId } });
        } else {
          this.submitting.set(false);
        }
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Failed to submit order', detail: 'Please try again.' });
      }
    });
  }

  canDeactivate(): boolean {
    if (this.cartService.cart().length === 0) return true;
    return confirm('You have items in your cart. Leave without completing this order?');
  }
}

import { ChangeDetectionStrategy, Component, inject, effect, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DecimalPipe, NgOptimizedImage } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CarouselModule } from 'primeng/carousel';
import { MessageService } from 'primeng/api';
import { OrderService } from '../../core/services/order.service';
import { CartService } from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';
import { ProductDisplayNamePipe } from '../../core/pipes/product-display-name.pipe';
import { CanComponentDeactivate } from './order-create.guard';

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [ButtonModule, CardModule, CarouselModule, DecimalPipe, NgOptimizedImage, RouterLink, ProductDisplayNamePipe],
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

  readonly products = toSignal(this.productService.getAll(), { initialValue: null });
  readonly submitting = signal(false);

  constructor() {
    effect(() => {
      this.cartService.cart();
      Promise.resolve().then(() => window.dispatchEvent(new Event('resize')));
    });
  }

  readonly responsiveOptions = [
    { breakpoint: '1024px', numVisible: 3, numScroll: 1 },
    { breakpoint: '768px',  numVisible: 2, numScroll: 1 },
    { breakpoint: '560px',  numVisible: 1, numScroll: 1 }
  ];

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
        }
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Failed to submit order' });
      }
    });
  }

  canDeactivate(): boolean {
    if (this.cartService.cart().length === 0) return true;
    return confirm('You have items in your cart. Leave without completing this order?');
  }
}
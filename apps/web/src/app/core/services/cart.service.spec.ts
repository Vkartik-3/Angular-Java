import { CartService } from './cart.service';
import { Product } from '../models/product.model';

const product = (id: string, price: number): Product =>
  ({ id, category: 'vinyl', price, imageUrl: '', attributes: { artist: 'A', title: id } });

describe('CartService', () => {
  let cart: CartService;
  beforeEach(() => (cart = new CartService()));

  it('merges repeated adds and computes totals', () => {
    cart.add(product('a', 10));
    cart.add(product('a', 10));
    cart.add(product('b', 5.5));
    expect(cart.count()).toBe(3);
    expect(cart.total()).toBe(25.5);
    expect(cart.cart().length).toBe(2);
  });

  it('removes an item when its quantity drops below one', () => {
    cart.add(product('a', 10));
    cart.setQuantity('a', 0);
    expect(cart.cart()).toEqual([]);
  });
});

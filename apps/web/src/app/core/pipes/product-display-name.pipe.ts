import { Pipe, PipeTransform } from '@angular/core';
import { Product } from '../models/product.model';

@Pipe({ name: 'productDisplayName', standalone: true, pure: true })
export class ProductDisplayNamePipe implements PipeTransform {
  transform(product: Product): string {
    if (product.category === 'vinyl') {
      return `${product.attributes['artist']} — ${product.attributes['title']}`;
    }
    return `${product.attributes['manufacturer']} — ${product.attributes['name']}`;
  }
}
import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  RouterLink
} from '@angular/router';

import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';

import { LoadingComponent } from '../../shared/loading/loading';

import { Product } from '../../core/models/app.models';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    RouterLink,
    LoadingComponent
  ],
  templateUrl: './products.html'
})
export class ProductsComponent {

  products =
    signal<Product[]>([]);

  loading =
    signal(true);

  error =
    signal('');

  readonly cart =
    inject(CartService);

  private readonly productService =
    inject(ProductService);

  constructor() {
    this.load();
  }

  load(): void {

    this.loading.set(true);

    this.productService
      .getAll()
      .subscribe({

        next: data => {

          this.products.set(data);
          this.loading.set(false);
        },

        error: () => {

          this.error.set(
            'Unable to load products.'
          );

          this.loading.set(false);
        }
      });
  }

  add(product: Product): void {
    this.cart.add(product);
  }
}

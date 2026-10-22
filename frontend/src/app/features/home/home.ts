
import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import {
  CartService,
  BackendCartItem
} from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/app.models';
import { LoadingComponent } from '../../shared/loading/loading';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [LoadingComponent, DecimalPipe],
  templateUrl: './home.html'
})
export class HomeComponent {

  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);

  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);

  constructor() {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set('');

    this.productService.getAll().subscribe({
      next: products => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(
          'We could not load products. Please try again.'
        );
        this.loading.set(false);
      }
    });
  }

  cartItem(
    productId: number | undefined
  ): BackendCartItem | undefined {

    if (!this.auth.hasToken() || productId == null) {
      return undefined;
    }

    return this.cart.cartItems().find(
      item => item.productId === productId
    );
  }

  add(product: Product): void {

    if (!this.auth.hasToken()) {
      this.router.navigate(['/login'], {
        queryParams: {
          message: 'Please sign in to add items to your cart.',
          returnUrl: '/'
        }
      });
      return;
    }

    if (product.id == null || product.quantity <= 0) {
      return;
    }

    this.cart.add(product);
  }

  increase(product: Product): void {

    const item = this.cartItem(product.id);

    if (!item) {
      this.add(product);
      return;
    }

    if (item.quantity >= product.quantity) {
      return;
    }

    this.cart.update(
      item.itemId,
      item.quantity + 1
    );
  }

  decrease(product: Product): void {

    const item = this.cartItem(product.id);

    if (!item) {
      return;
    }

    if (item.quantity <= 1) {
      this.cart.remove(item.itemId);
    } else {
      this.cart.update(
        item.itemId,
        item.quantity - 1
      );
    }
  }

  productIcon(name: string): string {

    const value = name.toLowerCase();

    if (
      value.includes('phone') ||
      value.includes('mobile')
    ) {
      return '📱';
    }

    if (
      value.includes('laptop') ||
      value.includes('computer')
    ) {
      return '💻';
    }

    if (
      value.includes('headphone') ||
      value.includes('audio')
    ) {
      return '🎧';
    }

    if (value.includes('watch')) return '⌚';
    if (value.includes('book')) return '📚';
    if (value.includes('shoe')) return '👟';

    return '🛍️';
  }
}

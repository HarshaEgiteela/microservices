import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
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
  readonly notice = signal('');

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
        this.error.set('We could not load products. Please try again.');
        this.loading.set(false);
      }
    });
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

    this.cart.add(product);
    this.notice.set(`${product.name} added to your cart.`);
    setTimeout(() => this.notice.set(''), 2500);
  }

  productIcon(name: string): string {
    const value = name.toLowerCase();
    if (value.includes('phone') || value.includes('mobile')) return '📱';
    if (value.includes('laptop') || value.includes('computer')) return '💻';
    if (value.includes('headphone') || value.includes('audio')) return '🎧';
    if (value.includes('watch')) return '⌚';
    if (value.includes('book')) return '📚';
    if (value.includes('shoe')) return '👟';
    return '🛍️';
  }
}


import {
  Component,
  inject,
  signal
} from '@angular/core';

import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';

import {
  ProductService,
  ProductFilters
} from '../../core/services/product.service';

import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { LoadingComponent } from '../../shared/loading/loading';
import { Product } from '../../core/models/app.models';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    FormsModule,
    DecimalPipe,
    RouterLink,
    LoadingComponent
  ],
  templateUrl: './products.html'
})
export class ProductsComponent {

  readonly products = signal<Product[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);

  readonly pageSize = 12;
  readonly cart = inject(CartService);

  private readonly productService = inject(ProductService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  search = '';
  category = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;

  sort: NonNullable<ProductFilters['sort']> = 'name_asc';

  private requestVersion = 0;

  readonly categories = [
    'Electronics',
    'Fashion',
    'Home & Kitchen',
    'Beauty',
    'Books',
    'Sports',
    'Toys',
    'Accessories',
    'Other'
  ];

  constructor() {
    this.load();
  }

  load(page: number = 0): void {
    if (page < 0) return;

    if (
      this.minPrice != null &&
      (!Number.isFinite(this.minPrice) || this.minPrice < 0)
    ) {
      this.error.set('Enter a valid minimum price.');
      return;
    }

    if (
      this.maxPrice != null &&
      (!Number.isFinite(this.maxPrice) || this.maxPrice < 0)
    ) {
      this.error.set('Enter a valid maximum price.');
      return;
    }

    if (
      this.minPrice != null &&
      this.maxPrice != null &&
      this.minPrice > this.maxPrice
    ) {
      this.error.set(
        'Minimum price cannot exceed maximum price.'
      );
      return;
    }

    const version = ++this.requestVersion;

    this.loading.set(true);
    this.error.set('');

    this.productService.getProducts({
      search: this.search,
      category: this.category,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      sort: this.sort,
      page,
      size: this.pageSize
    }).subscribe({
      next: response => {
        if (version !== this.requestVersion) return;

        this.products.set(response.content);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.currentPage.set(response.number);
        this.loading.set(false);
      },
      error: () => {
        if (version !== this.requestVersion) return;

        this.products.set([]);
        this.error.set('Unable to load products.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.load(0);
  }

  resetFilters(): void {
    this.search = '';
    this.category = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.sort = 'name_asc';

    this.load(0);
  }

  nextPage(): void {
    if (
      !this.loading() &&
      this.currentPage() + 1 < this.totalPages()
    ) {
      this.load(this.currentPage() + 1);
    }
  }

  previousPage(): void {
    if (!this.loading() && this.currentPage() > 0) {
      this.load(this.currentPage() - 1);
    }
  }

  add(product: Product): void {
    if (!this.auth.hasToken()) {
      this.router.navigate(['/login'], {
        queryParams: {
          message: 'Please sign in to add items to your cart.',
          returnUrl: '/products'
        }
      });
      return;
    }

    if (product.id == null || product.quantity <= 0) return;

    this.cart.add(product);
  }

  productIcon(name: string): string {
    const value = name.toLowerCase();

    if (value.includes('phone')) return '📱';
    if (value.includes('laptop')) return '💻';
    if (value.includes('headphone')) return '🎧';
    if (value.includes('watch')) return '⌚';
    if (value.includes('book')) return '📚';
    if (value.includes('shoe')) return '👟';

    return '🛍️';
  }
}

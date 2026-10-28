
import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  imports: [LoadingComponent, DecimalPipe, FormsModule],
  templateUrl: './home.html'
})
export class HomeComponent {

  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);

  readonly pageSize = 12;

  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);

  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);

  searchInput = '';
  appliedSearch = '';
  category = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  private appliedMinPrice: number | undefined;
  private appliedMaxPrice: number | undefined;

  sort: 'name_asc' | 'name_desc' | 'price_asc' | 'price_desc'
    = 'name_asc';

  openDropdown: 'category' | 'price' | 'sort' | null = null;
  filterError = '';
  private requestVersion = 0;

  readonly categories = [
    'Electronics', 'Fashion', 'Home & Kitchen', 'Beauty',
    'Books', 'Sports', 'Toys', 'Accessories', 'Other'
  ];

  constructor() {
    this.loadProducts();
  }

  toggleDropdown(name: 'category' | 'price' | 'sort'): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  searchProducts(): void {
    this.appliedSearch = this.searchInput.trim();
    this.loadProducts(0);
  }

  clearSearch(): void {
    this.searchInput = '';
    this.appliedSearch = '';
    this.loadProducts(0);
  }

  changeCategory(): void {
    this.openDropdown = null;
    this.loadProducts(0);
  }

  changeSort(): void {
    this.openDropdown = null;
    this.loadProducts(0);
  }

  applyPrice(): void {
    if (
      (this.minPrice != null &&
        (!Number.isFinite(this.minPrice) || this.minPrice < 0)) ||
      (this.maxPrice != null &&
        (!Number.isFinite(this.maxPrice) || this.maxPrice < 0)) ||
      (this.minPrice != null &&
        this.maxPrice != null &&
        this.minPrice > this.maxPrice)
    ) {
      this.filterError = 'Enter a valid price range.';
      return;
    }

    this.filterError = '';
    this.appliedMinPrice = this.minPrice ?? undefined;
    this.appliedMaxPrice = this.maxPrice ?? undefined;
    this.openDropdown = null;
    this.loadProducts(0);
  }

  clearPrice(): void {
    this.minPrice = null;
    this.maxPrice = null;
    this.appliedMinPrice = undefined;
    this.appliedMaxPrice = undefined;
    this.filterError = '';
    this.openDropdown = null;
    this.loadProducts(0);
  }

  clearFilters(): void {
    this.category = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.appliedMinPrice = undefined;
    this.appliedMaxPrice = undefined;
    this.sort = 'name_asc';
    this.filterError = '';
    this.openDropdown = null;
    this.loadProducts(0);
  }

  get hasFilters(): boolean {
    return !!this.category ||
      this.appliedMinPrice != null ||
      this.appliedMaxPrice != null ||
      this.sort !== 'name_asc';
  }

  loadProducts(page: number = 0): void {
    if (page < 0) return;

    const version = ++this.requestVersion;
    this.loading.set(true);
    this.error.set('');

    this.productService.getProducts({
      search: this.appliedSearch,
      category: this.category,
      minPrice: this.appliedMinPrice,
      maxPrice: this.appliedMaxPrice,
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
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.error.set(
          'We could not load products. Please try again.'
        );
        this.loading.set(false);
      }
    });
  }

  nextPage(): void {
    if (
      !this.loading() &&
      this.currentPage() + 1 < this.totalPages()
    ) {
      this.loadProducts(this.currentPage() + 1);
    }
  }

  previousPage(): void {
    if (!this.loading() && this.currentPage() > 0) {
      this.loadProducts(this.currentPage() - 1);
    }
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

    if (product.id == null || product.quantity <= 0) return;

    this.cart.add(product);
  }

  increase(product: Product): void {
    const item = this.cartItem(product.id);

    if (!item) {
      this.add(product);
      return;
    }

    if (item.quantity >= product.quantity) return;

    this.cart.update(item.itemId, item.quantity + 1);
  }

  decrease(product: Product): void {
    const item = this.cartItem(product.id);
    if (!item) return;

    if (item.quantity <= 1) {
      this.cart.remove(item.itemId);
    } else {
      this.cart.update(item.itemId, item.quantity - 1);
    }
  }

  productIcon(name: string): string {
    const value = name.toLowerCase();

    if (value.includes('phone') || value.includes('mobile')) {
      return '📱';
    }
    if (value.includes('laptop') || value.includes('computer')) {
      return '💻';
    }
    if (value.includes('headphone') || value.includes('audio')) {
      return '🎧';
    }
    if (value.includes('watch')) return '⌚';
    if (value.includes('book')) return '📚';
    if (value.includes('shoe')) return '👟';

    return '🛍️';
  }
}

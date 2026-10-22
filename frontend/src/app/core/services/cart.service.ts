
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AuthService } from './auth.service';
import { Product } from '../models/app.models';

export interface BackendCartItem {
  itemId: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface BackendCart {
  cartId: number;
  userId: number;
  items: BackendCartItem[];
  totalAmount: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private url = `${API_BASE_URL}/cart`;

  private items = signal<BackendCartItem[]>([]);
  readonly cartItems = this.items.asReadonly();

  readonly count = computed(() =>
    this.items().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.subtotal, 0)
  );

  readonly loading = signal(false);
  readonly error = signal('');

  constructor() {
    localStorage.removeItem('shopease-cart');

    if (this.auth.hasToken()) {
      this.load();
    }
  }

  load(): void {
    if (!this.auth.hasToken()) {
      this.reset();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.http.get<BackendCart>(this.url)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: cart => this.items.set(cart.items),
        error: () => {
          this.items.set([]);
          this.error.set('Unable to load your cart.');
        }
      });
  }

  add(product: Product): void {
    if (product.id == null || product.quantity <= 0) return;

    if (!this.auth.hasToken()) {
      this.error.set('Please log in to add products.');
      return;
    }

    this.error.set('');

    this.http.post<BackendCart>(`${this.url}/items`, {
      productId: product.id,
      quantity: 1
    }).subscribe({
      next: cart => this.items.set(cart.items),
      error: () => this.error.set('Unable to add product.')
    });
  }

  update(itemId: number, quantity: number): void {
    if (!Number.isFinite(quantity)) return;

    if (quantity <= 0) {
      this.remove(itemId);
      return;
    }

    this.http.put<BackendCart>(
      `${this.url}/items/${itemId}`,
      { quantity: Math.floor(quantity) }
    ).subscribe({
      next: cart => this.items.set(cart.items),
      error: () => this.error.set('Unable to update quantity.')
    });
  }

  remove(itemId: number): void {
    this.http.delete<void>(`${this.url}/items/${itemId}`)
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set('Unable to remove item.')
      });
  }

  clear(): void {
    this.http.delete<void>(this.url).subscribe({
      next: () => this.items.set([]),
      error: () => this.error.set('Unable to clear cart.')
    });
  }

  reset(): void {
    this.items.set([]);
    this.error.set('');
  }
}

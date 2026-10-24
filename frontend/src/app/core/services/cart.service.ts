
import {
  Injectable,
  computed,
  inject,
  signal
} from '@angular/core';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import { firstValueFrom } from 'rxjs';

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
  cartId: number | null;
  userId: number;
  items: BackendCartItem[];
  totalAmount: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly url = `${API_BASE_URL}/cart`;

  private readonly items = signal<BackendCartItem[]>([]);

  readonly cartItems = this.items.asReadonly();

  readonly count = computed(() =>
    this.items().reduce(
      (sum, item) => sum + item.quantity,
      0
    )
  );

  readonly total = computed(() =>
    this.items().reduce(
      (sum, item) => sum + item.subtotal,
      0
    )
  );

  readonly loading = signal(false);
  readonly error = signal('');

  private generation = 0;
  private pendingLoads = 0;

  constructor() {
    localStorage.removeItem('shopease-cart');

    if (this.auth.hasToken()) {
      this.load();
    }
  }

  private getErrorMessage(
    err: HttpErrorResponse,
    fallback: string
  ): string {

    console.error('Cart API error:', err);

    if (typeof err.error === 'string' && err.error.trim()) {
      return err.error;
    }

    if (err.error?.message) {
      return err.error.message;
    }

    return `${fallback} (HTTP ${err.status || 'Network Error'})`;
  }

  private applyCart(
    cart: BackendCart,
    generation: number
  ): void {
    if (generation !== this.generation) return;

    this.items.set(cart.items ?? []);
  }

  load(): void {
    void this.loadAsync().catch(() => {
      // Error is stored in the error signal.
    });
  }

  async loadAsync(): Promise<void> {
    if (!this.auth.hasToken()) {
      this.reset();
      return;
    }

    const generation = this.generation;

    this.pendingLoads++;
    this.loading.set(true);
    this.error.set('');

    try {
      const cart = await firstValueFrom(
        this.http.get<BackendCart>(this.url)
      );

      this.applyCart(cart, generation);

    } catch (err) {
      if (generation === this.generation) {
        this.error.set(
          this.getErrorMessage(
            err as HttpErrorResponse,
            'Unable to load your cart.'
          )
        );
      }

      throw err;

    } finally {
      if (generation === this.generation) {
        this.pendingLoads = Math.max(
          0,
          this.pendingLoads - 1
        );

        this.loading.set(this.pendingLoads > 0);
      }
    }
  }

  add(product: Product): void {
    if (product.id == null || product.quantity <= 0) {
      return;
    }

    if (!this.auth.hasToken()) {
      this.error.set('Please log in to add products.');
      return;
    }

    const generation = this.generation;
    this.error.set('');

    this.http.post<BackendCart>(
      `${this.url}/items`,
      {
        productId: product.id,
        quantity: 1
      }
    ).subscribe({
      next: cart => {
        this.applyCart(cart, generation);
      },

      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;

        this.error.set(
          this.getErrorMessage(
            err,
            'Unable to add product.'
          )
        );
      }
    });
  }

  update(itemId: number, quantity: number): void {
    void this.updateAsync(itemId, quantity).catch(() => {
      // Caller can handle the failure.
    });
  }

  async updateAsync(
    itemId: number,
    quantity: number
  ): Promise<void> {

    if (!Number.isFinite(quantity)) return;

    if (quantity <= 0) {
      await this.removeAsync(itemId);
      return;
    }

    const generation = this.generation;

    try {
      const cart = await firstValueFrom(
        this.http.put<BackendCart>(
          `${this.url}/items/${itemId}`,
          {
            quantity: Math.floor(quantity)
          }
        )
      );

      this.applyCart(cart, generation);

    } catch (err) {
      console.error('Cart quantity update failed:', err);

      // Do not create a global cart error for stock failures.
      // The cart component displays item-specific warnings.
      throw err;
    }
  }

  remove(itemId: number): void {
    void this.removeAsync(itemId).catch(() => {
      // Error is stored in the error signal.
    });
  }

  async removeAsync(itemId: number): Promise<void> {
    const generation = this.generation;

    try {
      await firstValueFrom(
        this.http.delete<void>(
          `${this.url}/items/${itemId}`
        )
      );

      if (generation !== this.generation) return;

      this.items.update(items =>
        items.filter(item => item.itemId !== itemId)
      );

      this.error.set('');

    } catch (err) {
      if (generation === this.generation) {
        this.error.set(
          this.getErrorMessage(
            err as HttpErrorResponse,
            'Unable to remove item.'
          )
        );
      }

      throw err;
    }
  }

  clear(): void {
    const generation = this.generation;

    this.error.set('');

    this.http.delete<void>(this.url).subscribe({
      next: () => {
        if (generation !== this.generation) return;

        this.items.set([]);
      },

      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;

        this.error.set(
          this.getErrorMessage(
            err,
            'Unable to clear cart.'
          )
        );
      }
    });
  }

  reset(): void {
    this.generation++;
    this.pendingLoads = 0;

    this.items.set([]);
    this.loading.set(false);
    this.error.set('');
  }
}


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

  load(): void {
    if (!this.auth.hasToken()) {
      this.reset();
      return;
    }

    const generation = this.generation;

    this.loading.set(true);
    this.error.set('');

    this.http.get<BackendCart>(this.url)
      .pipe(finalize(() => {
        if (generation === this.generation) {
          this.loading.set(false);
        }
      }))
      .subscribe({
        next: cart => {
          if (generation !== this.generation) return;

          this.items.set(cart.items ?? []);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;

          this.items.set([]);
          this.error.set(
            this.getErrorMessage(err, 'Unable to load your cart.')
          );
        }
      });
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
        if (generation !== this.generation) return;

        this.items.set(cart.items ?? []);
      },
      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;

        this.error.set(
          this.getErrorMessage(err, 'Unable to add product.')
        );
      }
    });
  }

  update(itemId: number, quantity: number): void {
    if (!Number.isFinite(quantity)) return;

    if (quantity <= 0) {
      this.remove(itemId);
      return;
    }

    const generation = this.generation;

    this.error.set('');

    this.http.put<BackendCart>(
      `${this.url}/items/${itemId}`,
      { quantity: Math.floor(quantity) }
    ).subscribe({
      next: cart => {
        if (generation !== this.generation) return;

        this.items.set(cart.items ?? []);
      },
      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;

        this.error.set(
          this.getErrorMessage(err, 'Unable to update quantity.')
        );
      }
    });
  }

  remove(itemId: number): void {
    const generation = this.generation;

    this.error.set('');

    this.http.delete<void>(
      `${this.url}/items/${itemId}`
    ).subscribe({
      next: () => {
        if (generation !== this.generation) return;

        this.load();
      },
      error: (err: HttpErrorResponse) => {
        if (generation !== this.generation) return;

        this.error.set(
          this.getErrorMessage(err, 'Unable to remove item.')
        );
      }
    });
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
          this.getErrorMessage(err, 'Unable to clear cart.')
        );
      }
    });
  }

  reset(): void {
    this.generation++;
    this.items.set([]);
    this.loading.set(false);
    this.error.set('');
  }
}

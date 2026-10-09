import { Injectable, computed, signal } from '@angular/core';
import { CartItem, Product } from '../models/app.models';

const CART_STORAGE_KEY = 'shopease-cart';

function readSavedItems(): CartItem[] {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as CartItem[];
    return Array.isArray(parsed) ? parsed.filter(
      item => item?.product?.id != null && Number(item.quantity) > 0
    ) : [];
  } catch {
    return [];
  }
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly items = signal<CartItem[]>(readSavedItems());
  readonly cartItems = this.items.asReadonly();

  readonly count = computed(() =>
    this.items().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.product.price * item.quantity, 0)
  );

  add(product: Product): void {
    if (product.id == null || product.quantity <= 0) return;

    this.items.update(items => {
      const existing = items.find(item => item.product.id === product.id);
      if (existing) {
        return items.map(item => item.product.id === product.id
          ? { ...item, product, quantity: Math.min(item.quantity + 1, product.quantity) }
          : item);
      }
      return [...items, { product, quantity: 1 }];
    });
    this.persist();
  }

  update(productId: number, quantity: number): void {
    if (!Number.isFinite(quantity) || quantity <= 0) {
      this.remove(productId);
      return;
    }
    this.items.update(items => items.map(item =>
      item.product.id === productId
        ? { ...item, quantity: Math.min(Math.floor(quantity), item.product.quantity) }
        : item
    ));
    this.persist();
  }

  remove(productId: number): void {
    this.items.update(items => items.filter(item => item.product.id !== productId));
    this.persist();
  }

  clear(): void {
    this.items.set([]);
    this.persist();
  }

  private persist(): void {
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this.items()));
    } catch {
      // Cart remains usable in memory if browser storage is unavailable.
    }
  }
}

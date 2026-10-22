
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Product,
  Order,
  OrderStatus,
  User
} from '../../core/models/app.models';

import { ProductService } from '../../core/services/product.service';
import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { LoadingComponent } from '../../shared/loading/loading';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    FormsModule,
    LoadingComponent
  ],
  templateUrl: './admin.html'
})
export class AdminComponent {

  readonly users = signal<User[]>([]);
  readonly products = signal<Product[]>([]);
  readonly orders = signal<Order[]>([]);

  readonly loading = signal(true);
  readonly message = signal('');
  readonly error = signal('');

  readonly updatingOrderId = signal<number | null>(null);

  editingId: number | null = null;

  form: Product = {
    name: '',
    price: 0,
    quantity: 0,
    description: ''
  };

  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    let done = 0;

    const finish = () => {
      done++;

      if (done === 3) {
        this.loading.set(false);
      }
    };

    this.productService.getAll().subscribe({
      next: data => {
        this.products.set(data);
        finish();
      },
      error: () => {
        this.error.set('Unable to load products.');
        finish();
      }
    });

    this.orderService.getAll().subscribe({
      next: data => {
        this.orders.set(data);
        finish();
      },
      error: () => {
        this.error.set('Unable to load orders.');
        finish();
      }
    });

    this.userService.getAll().subscribe({
      next: data => {
        this.users.set(data);
        finish();
      },
      error: () => {
        this.error.set('Unable to load users.');
        finish();
      }
    });
  }

  // Find customer details from the loaded users.
  customerName(userId: number): string {
    const user = this.users().find(u => u.id === userId);

    return user?.name ?? `User #${userId}`;
  }

  customerEmail(userId: number): string {
    const user = this.users().find(u => u.id === userId);

    return user?.email ?? '';
  }

  // Allowed next status in the normal delivery flow.
  nextStatus(status: OrderStatus): OrderStatus | null {
    switch (status) {
      case 'PLACED':
        return 'CONFIRMED';

      case 'CONFIRMED':
        return 'PROCESSING';

      case 'PROCESSING':
        return 'SHIPPED';

      case 'SHIPPED':
        return 'DELIVERED';

      default:
        return null;
    }
  }

  // Advance one step only.
  advanceStatus(order: Order): void {
    const next = this.nextStatus(order.status);

    if (
      order.id == null ||
      next === null ||
      this.updatingOrderId() !== null
    ) {
      return;
    }

    this.updatingOrderId.set(order.id);
    this.error.set('');
    this.message.set('');

    this.orderService.updateStatus(order.id, next).subscribe({
      next: updated => {
        this.orders.update(orders =>
          orders.map(item =>
            item.id === updated.id ? updated : item
          )
        );

        this.message.set(
          `Order #${updated.id} updated to ${updated.status}.`
        );

        this.updatingOrderId.set(null);
      },

      error: err => {
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to update order status.'
        );

        this.updatingOrderId.set(null);
      }
    });
  }

  edit(product: Product): void {
    this.editingId = product.id ?? null;
    this.form = { ...product };
  }

  reset(): void {
    this.editingId = null;

    this.form = {
      name: '',
      price: 0,
      quantity: 0,
      description: ''
    };
  }

  saveProduct(): void {
    this.error.set('');
    this.message.set('');

    const request = this.editingId !== null
      ? this.productService.update(this.editingId, this.form)
      : this.productService.create(this.form);

    request.subscribe({
      next: () => {
        this.message.set(
          this.editingId !== null
            ? 'Product updated.'
            : 'Product created.'
        );

        this.reset();
        this.load();
      },

      error: err => {
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to save product.'
        );
      }
    });
  }

  deleteProduct(id: number): void {
    this.error.set('');
    this.message.set('');

    this.productService.delete(id).subscribe({
      next: () => {
        this.message.set('Product deleted.');
        this.load();
      },

      error: err => {
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to delete product.'
        );
      }
    });
  }
}

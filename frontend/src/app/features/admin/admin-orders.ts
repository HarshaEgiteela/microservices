
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  Order,
  OrderStatus,
  User
} from '../../core/models/app.models';

import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { LoadingComponent } from '../../shared/loading/loading';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [
    RouterLink,
    LoadingComponent
  ],
  templateUrl: './admin-orders.html'
})
export class AdminOrdersComponent {

  readonly orders = signal<Order[]>([]);
  readonly users = signal<User[]>([]);

  readonly loading = signal(true);
  readonly message = signal('');
  readonly error = signal('');

  readonly expandedOrderId = signal<number | null>(null);
  readonly updatingOrderId = signal<number | null>(null);

  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  constructor() {
    this.load();
  }

  load(): void {
    if (this.updatingOrderId() !== null) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    let completed = 0;

    const finish = () => {
      completed++;

      if (completed === 2) {
        this.loading.set(false);
      }
    };

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
        this.error.set('Unable to load customer details.');
        finish();
      }
    });
  }

  customerName(userId: number): string {
    const user = this.users().find(u => u.id === userId);

    return user?.name ?? `User #${userId}`;
  }

  customerEmail(userId: number): string {
    const user = this.users().find(u => u.id === userId);

    return user?.email ?? '';
  }

  toggleOrder(orderId: number): void {
    this.expandedOrderId.update(current =>
      current === orderId ? null : orderId
    );
  }

  // Normal sequential delivery flow.
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

  // Frontend restriction. Backend validates the final decision.
  canCancel(status: OrderStatus): boolean {
    return status === 'PLACED' ||
      status === 'CONFIRMED' ||
      status === 'PROCESSING';
  }

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
        this.replaceOrder(updated);

        this.message.set(
          `Order #${updated.id} updated to ${updated.status}.`
        );

        this.updatingOrderId.set(null);
      },

      error: err => {
        this.error.set(
          this.errorMessage(err, 'Unable to update order status.')
        );

        this.updatingOrderId.set(null);
      }
    });
  }

  cancelOrder(order: Order): void {
    if (
      order.id == null ||
      !this.canCancel(order.status) ||
      this.updatingOrderId() !== null
    ) {
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to cancel Order #${order.id}?`
    );

    if (!confirmed) {
      return;
    }

    this.updatingOrderId.set(order.id);
    this.error.set('');
    this.message.set('');

    this.orderService.cancelOrder(order.id).subscribe({
      next: updated => {
        this.replaceOrder(updated);

        this.message.set(
          `Order #${updated.id} updated to ${updated.status}.`
        );

        this.updatingOrderId.set(null);
      },

      error: err => {
        this.error.set(
          this.errorMessage(err, 'Unable to cancel order.')
        );

        this.updatingOrderId.set(null);
      }
    });
  }

  private replaceOrder(updated: Order): void {
    this.orders.update(orders =>
      orders.map(order =>
        order.id === updated.id ? updated : order
      )
    );
  }

  private errorMessage(err: any, fallback: string): string {
    return typeof err?.error === 'string'
      ? err.error
      : err?.error?.message || fallback;
  }
}

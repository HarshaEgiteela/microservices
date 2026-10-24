
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { LoadingComponent } from '../../shared/loading/loading';
import { Order, OrderStatus } from '../../core/models/app.models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    RouterLink,
    LoadingComponent,
    DatePipe
  ],
  templateUrl: './orders.html'
})
export class OrdersComponent {

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly success = signal('');

  readonly processingOrderId = signal<number | null>(null);

  private readonly service = inject(OrderService);
  private readonly auth = inject(AuthService);

  readonly isAdmin = this.auth.isAdmin();

  constructor() {
    this.load();
  }

  // ==========================================
  // LOAD ORDERS
  // ==========================================

  load(): void {
    this.loading.set(true);
    this.error.set('');

    const request = this.isAdmin
      ? this.service.getAll()
      : this.service.getMyOrders();

    request.subscribe({
      next: data => {
        this.orders.set(data);
        this.loading.set(false);
      },

      error: err => {
        this.error.set(
          this.getErrorMessage(
            err,
            'Unable to load orders.'
          )
        );

        this.loading.set(false);
      }
    });
  }

  // ==========================================
  // CHECK WHETHER ORDER CAN BE CANCELLED
  // ==========================================

  canCancel(order: Order): boolean {
    return [
      'PLACED',
      'CONFIRMED',
      'PROCESSING'
    ].includes(order.status);
  }

  // ==========================================
  // ADMIN: CHECK NEXT STATUS AVAILABILITY
  // ==========================================

  canMoveToNextStatus(order: Order): boolean {
    return this.isAdmin && [
      'PLACED',
      'CONFIRMED',
      'PROCESSING',
      'SHIPPED'
    ].includes(order.status);
  }

  // ==========================================
  // DETERMINE NEXT ORDER STATUS
  // ==========================================

  nextStatus(status: OrderStatus): OrderStatus | null {

    const transitions: Partial<Record<OrderStatus, OrderStatus>> = {
      PLACED: 'CONFIRMED',
      CONFIRMED: 'PROCESSING',
      PROCESSING: 'SHIPPED',
      SHIPPED: 'DELIVERED'
    };

    return transitions[status] ?? null;
  }

  // ==========================================
  // CUSTOMER / ADMIN: CANCEL ORDER
  // ==========================================

  cancelOrder(order: Order): void {

    if (!this.canCancel(order)) {
      return;
    }

    if (this.processingOrderId() !== null) {
      return;
    }

    const confirmed = window.confirm(
      `Are you sure you want to cancel order #${order.id}?`
    );

    if (!confirmed) {
      return;
    }

    this.processingOrderId.set(order.id);
    this.error.set('');
    this.success.set('');

    const request = this.isAdmin
      ? this.service.cancelOrder(order.id)
      : this.service.cancelMyOrder(order.id);

    request.subscribe({
      next: () => {
        this.processingOrderId.set(null);

        this.success.set(
          `Cancellation requested for order #${order.id}.`
        );

        this.load();
      },

      error: err => {
        this.processingOrderId.set(null);

        this.error.set(
          this.getErrorMessage(
            err,
            'Unable to cancel order.'
          )
        );
      }
    });
  }

  // ==========================================
  // ADMIN: MOVE ORDER TO NEXT STATUS
  // ==========================================

  moveToNextStatus(order: Order): void {

    if (!this.canMoveToNextStatus(order)) {
      return;
    }

    if (this.processingOrderId() !== null) {
      return;
    }

    const status = this.nextStatus(order.status);

    if (status === null) {
      return;
    }

    this.processingOrderId.set(order.id);
    this.error.set('');
    this.success.set('');

    this.service.updateStatus(order.id, status).subscribe({
      next: () => {
        this.processingOrderId.set(null);

        this.success.set(
          `Order #${order.id} updated to ${status}.`
        );

        this.load();
      },

      error: err => {
        this.processingOrderId.set(null);

        this.error.set(
          this.getErrorMessage(
            err,
            'Unable to update order status.'
          )
        );
      }
    });
  }

  // ==========================================
  // ERROR MESSAGE HANDLING
  // ==========================================

  private getErrorMessage(
    err: unknown,
    fallback: string
  ): string {

    if (typeof err !== 'object' || err === null) {
      return fallback;
    }

    const httpError = err as {
      error?: string | {
        message?: string;
      };
    };

    if (typeof httpError.error === 'string') {
      return httpError.error || fallback;
    }

    if (
      httpError.error &&
      typeof httpError.error.message === 'string'
    ) {
      return httpError.error.message;
    }

    return fallback;
  }
}

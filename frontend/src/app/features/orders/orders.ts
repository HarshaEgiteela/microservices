
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { LoadingComponent } from '../../shared/loading/loading';
import { Order } from '../../core/models/app.models';

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

  private readonly service = inject(OrderService);
  private readonly auth = inject(AuthService);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    const request = this.auth.isAdmin()
      ? this.service.getAll()
      : this.service.getMyOrders();

    request.subscribe({
      next: data => {
        this.orders.set(data);
        this.loading.set(false);
      },

      error: err => {
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to load orders.'
        );

        this.loading.set(false);
      }
    });
  }
}

import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  RouterLink
} from '@angular/router';

import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

import { LoadingComponent } from '../../shared/loading/loading';

import { Order } from '../../core/models/app.models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    RouterLink,
    LoadingComponent
  ],
  templateUrl: './orders.html'
})
export class OrdersComponent {

  orders =
    signal<Order[]>([]);

  loading =
    signal(true);

  error =
    signal('');

  private readonly service =
    inject(OrderService);

  private readonly auth =
    inject(AuthService);

  constructor() {
    this.load();
  }

  load(): void {

    this.service
      .getAll()
      .subscribe({

        next: data => {

          const userId =
            this.auth.getUserId();

          this.orders.set(
            this.auth.isAdmin() || !userId
              ? data
              : data.filter(
                order =>
                  order.userId === userId
              )
          );

          this.loading.set(false);
        },

        error: err => {

          this.error.set(
            err?.error ||
            'Unable to load orders.'
          );

          this.loading.set(false);
        }
      });
  }
}

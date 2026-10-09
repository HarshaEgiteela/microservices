import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  Router,
  RouterLink
} from '@angular/router';

import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html'
})
export class CartComponent {

  readonly cart =
    inject(CartService);

  message =
    signal('');

  error =
    signal('');

  placing =
    signal(false);

  private readonly orders =
    inject(OrderService);

  private readonly auth =
    inject(AuthService);

  private readonly router =
    inject(Router);

  update(
    productId: number,
    value: string
  ): void {

    this.cart.update(
      productId,
      Number(value)
    );
  }

  remove(productId: number): void {
    this.cart.remove(productId);
  }

  checkout(): void {

    const userId =
      this.auth.getUserId();

    if (!userId) {

      this.error.set(
        'Your account ID is not available. Register/login again.'
      );

      return;
    }

    if (!this.cart.cartItems().length) {
      return;
    }

    this.error.set('');
    this.placing.set(true);

    const items =
      [...this.cart.cartItems()];

    let completed = 0;
    const failures: string[] = [];

    items.forEach(item => {

      this.orders
        .placeOrder(
          userId,
          item.product.id!,
          item.quantity
        )
        .subscribe({

          next: () => {

            completed++;

            this.finishIfDone(
              items.length,
              completed,
              failures
            );
          },

          error: err => {

            failures.push(
              err?.error ||
              `Product ${item.product.id} failed`
            );

            completed++;

            this.finishIfDone(
              items.length,
              completed,
              failures
            );
          }
        });
    });
  }

  private finishIfDone(
    total: number,
    completed: number,
    failures: string[]
  ): void {

    if (completed !== total) {
      return;
    }

    this.placing.set(false);

    if (failures.length) {

      this.error.set(
        failures.join(' | ')
      );

      return;
    }

    this.cart.clear();

    this.message.set(
      'Order placed successfully.'
    );

    setTimeout(
      () => this.router.navigate(['/orders']),
      700
    );
  }
}

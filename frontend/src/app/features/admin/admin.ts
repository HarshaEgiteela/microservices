import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

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

  users =
    signal<User[]>([]);

  products =
    signal<Product[]>([]);

  orders =
    signal<Order[]>([]);

  loading =
    signal(true);

  message =
    signal('');

  error =
    signal('');

  editingId: number | null = null;

  form: Product = {
    name: '',
    price: 0,
    quantity: 0,
    description: ''
  };

  readonly statuses: OrderStatus[] = [
    'PLACED',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
  ];

  private readonly productService =
    inject(ProductService);

  private readonly orderService =
    inject(OrderService);

  private readonly userService =
    inject(UserService);

  constructor() {
    this.load();
  }

  load(): void {

    this.loading.set(true);

    let done = 0;

    const finish = () => {

      done++;

      if (done === 3) {
        this.loading.set(false);
      }
    };

    this.productService
      .getAll()
      .subscribe({
        next: data => {
          this.products.set(data);
          finish();
        },
        error: () => finish()
      });

    this.orderService
      .getAll()
      .subscribe({
        next: data => {
          this.orders.set(data);
          finish();
        },
        error: () => finish()
      });

    this.userService
      .getAll()
      .subscribe({
        next: data => {
          this.users.set(data);
          finish();
        },
        error: () => finish()
      });
  }

  edit(product: Product): void {

    this.editingId =
      product.id ?? null;

    this.form = {
      ...product
    };
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

    const request =
      this.editingId
        ? this.productService.update(
          this.editingId,
          this.form
        )
        : this.productService.create(
          this.form
        );

    request.subscribe({

      next: () => {

        this.message.set(
          this.editingId
            ? 'Product updated.'
            : 'Product created.'
        );

        this.reset();
        this.load();
      },

      error: err => {

        this.error.set(
          err?.error ||
          'Unable to save product.'
        );
      }
    });
  }

  deleteProduct(id: number): void {

    this.productService
      .delete(id)
      .subscribe({

        next: () => {

          this.message.set(
            'Product deleted.'
          );

          this.load();
        },

        error: err => {

          this.error.set(
            err?.error ||
            'Unable to delete product.'
          );
        }
      });
  }

  updateStatus(
    order: Order,
    status: OrderStatus
  ): void {

    if (!order.id) {
      return;
    }

    this.orderService
      .updateStatus(
        order.id,
        status
      )
      .subscribe({

        next: updated => {

          this.orders.update(
            items =>
              items.map(
                item =>
                  item.id === updated.id
                    ? updated
                    : item
              )
          );

          this.message.set(
            'Order status updated.'
          );
        },

        error: err => {

          this.error.set(
            err?.error ||
            'Invalid status transition.'
          );
        }
      });
  }
}

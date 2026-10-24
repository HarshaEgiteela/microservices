
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Product,
  Order,
  User
} from '../../core/models/app.models';

import { ProductService } from '../../core/services/product.service';
import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { LoadingComponent } from '../../shared/loading/loading';

type ProductForm = Omit<Product, 'price' | 'quantity'> & {
  price: number | null;
  quantity: number | null;
};

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

  editingId: number | null = null;

  form: ProductForm = this.emptyForm();

  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  constructor() {
    this.load();
  }

  private emptyForm(): ProductForm {
    return {
      name: '',
      price: null,
      quantity: null,
      description: '',
      active: true
    };
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    let completed = 0;

    const finish = () => {
      completed++;

      if (completed === 3) {
        this.loading.set(false);
      }
    };

    // Load products
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

    // Load orders for dashboard statistics
    this.orderService.getAll().subscribe({
      next: data => {
        this.orders.set(data);
        finish();
      },

      error: () => {
        this.error.set('Unable to load order statistics.');
        finish();
      }
    });

    // Load users
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

  // Edit existing product
  edit(product: Product): void {
    this.editingId = product.id ?? null;

    this.form = {
      name: product.name,
      price: product.price,
      quantity: product.quantity,
      description: product.description ?? '',
      active: product.active ?? true
    };

    this.error.set('');
    this.message.set('');
  }

  // Reset product form
  reset(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.error.set('');
  }

  // Create or update product
  saveProduct(): void {
    this.error.set('');
    this.message.set('');

    const isEditing = this.editingId !== null;

    if (!this.form.name.trim()) {
      this.error.set('Please enter a product name.');
      return;
    }

    if (
      this.form.price === null ||
      !Number.isFinite(this.form.price) ||
      this.form.price < 0
    ) {
      this.error.set('Please enter a valid price.');
      return;
    }

    if (
      this.form.quantity === null ||
      !Number.isInteger(this.form.quantity) ||
      this.form.quantity < 0
    ) {
      this.error.set('Please enter a valid whole-number quantity.');
      return;
    }

    const payload: Product = {
      name: this.form.name.trim(),
      price: this.form.price,
      quantity: this.form.quantity,
      description: this.form.description ?? '',
      active: this.form.active ?? true
    };

    const request = isEditing
      ? this.productService.update(this.editingId!, payload)
      : this.productService.create(payload);

    request.subscribe({
      next: () => {
        this.reset();
        this.load();

        this.message.set(
          isEditing
            ? 'Product updated successfully.'
            : 'Product created successfully.'
        );
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

  // Delete product
  deleteProduct(id: number): void {
    if (!window.confirm('Are you sure you want to delete this product?')) {
      return;
    }

    this.error.set('');
    this.message.set('');

    this.productService.delete(id).subscribe({
      next: () => {
        this.load();
        this.message.set('Product deleted successfully.');
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

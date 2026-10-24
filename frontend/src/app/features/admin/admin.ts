
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

  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  // Add Product form
  form: ProductForm = this.emptyForm();

  // Edit Product popup
  showEditPopup = false;
  editingId: number | null = null;

  editForm: ProductForm = this.emptyForm();
  editError = '';
  saving = false;

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

    // Load orders
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

  // Reset only Add Product form
  reset(): void {
    this.form = this.emptyForm();
    this.error.set('');
  }

  // Add Product only
  saveProduct(): void {
    this.error.set('');
    this.message.set('');

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
      active: true
    };

    this.productService.create(payload).subscribe({
      next: () => {
        this.reset();
        this.load();
        this.message.set('Product created successfully.');
      },
      error: err => {
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to create product.'
        );
      }
    });
  }

  // Open Edit Product popup
  edit(product: Product): void {
    if (product.id == null) {
      this.error.set('Invalid product ID.');
      return;
    }

    this.editingId = product.id;

    this.editForm = {
      name: product.name,
      price: product.price,
      quantity: product.quantity,
      description: product.description ?? '',
      active: product.active ?? true
    };

    this.editError = '';
    this.message.set('');
    this.showEditPopup = true;
  }

  // Close Edit Product popup
  closeEditPopup(): void {
    if (this.saving) {
      return;
    }

    this.showEditPopup = false;
    this.editingId = null;
    this.editForm = this.emptyForm();
    this.editError = '';
  }

  // Update product from popup
  updateProduct(): void {
    if (this.editingId === null || this.saving) {
      return;
    }

    this.editError = '';

    if (!this.editForm.name.trim()) {
      this.editError = 'Please enter a product name.';
      return;
    }

    if (
      this.editForm.price === null ||
      !Number.isFinite(this.editForm.price) ||
      this.editForm.price < 0
    ) {
      this.editError = 'Please enter a valid price.';
      return;
    }

    if (
      this.editForm.quantity === null ||
      !Number.isInteger(this.editForm.quantity) ||
      this.editForm.quantity < 0
    ) {
      this.editError = 'Please enter a valid whole-number quantity.';
      return;
    }

    const payload: Product = {
      name: this.editForm.name.trim(),
      price: this.editForm.price,
      quantity: this.editForm.quantity,
      description: this.editForm.description ?? '',
      active: this.editForm.active ?? true
    };

    this.saving = true;

    this.productService.update(this.editingId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.closeEditPopup();
        this.load();
        this.message.set('Product updated successfully.');
      },
      error: err => {
        this.saving = false;

        this.editError =
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to update product.';
      }
    });
  }

  // Delete Product
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

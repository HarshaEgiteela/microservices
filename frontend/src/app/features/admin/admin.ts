
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
  readonly productsLoading = signal(false);

  readonly message = signal('');
  readonly error = signal('');
  readonly productError = signal('');

  readonly totalProducts = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);

  readonly pageSize = 20;

  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);

  readonly categories = [
    'Electronics',
    'Fashion',
    'Home & Kitchen',
    'Beauty',
    'Books',
    'Sports',
    'Toys',
    'Accessories',
    'Other'
  ];

  search = '';
  category = '';

  form: ProductForm = this.emptyForm();

  showEditPopup = false;
  editingId: number | null = null;
  editForm: ProductForm = this.emptyForm();
  editError = '';
  saving = false;
  creating = false;

  private productRequestVersion = 0;

  constructor() {
    this.load();
  }

  private emptyForm(): ProductForm {
    return {
      name: '',
      price: null,
      quantity: null,
      description: '',
      category: '',
      imageUrl: '',
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

    this.loadProducts(this.currentPage(), finish);

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

  loadProducts(
    page: number = 0,
    onComplete?: () => void
  ): void {

    const version = ++this.productRequestVersion;

    this.productsLoading.set(true);
    this.productError.set('');

    this.productService.getProducts({
      search: this.search,
      category: this.category,
      page,
      size: this.pageSize,
      sort: 'name_asc'
    }).subscribe({

      next: response => {

        if (version === this.productRequestVersion) {
          this.products.set(response.content);
          this.totalProducts.set(response.totalElements);
          this.totalPages.set(response.totalPages);
          this.currentPage.set(response.number);
          this.productsLoading.set(false);
        }

        onComplete?.();
      },

      error: () => {

        if (version === this.productRequestVersion) {
          this.products.set([]);
          this.productError.set('Unable to load products.');
          this.productsLoading.set(false);
        }

        onComplete?.();
      }
    });
  }

  applyProductFilters(): void {
    this.loadProducts(0);
  }

  clearProductFilters(): void {
    this.search = '';
    this.category = '';
    this.loadProducts(0);
  }

  nextProductPage(): void {
    if (
      !this.productsLoading() &&
      this.currentPage() + 1 < this.totalPages()
    ) {
      this.loadProducts(this.currentPage() + 1);
    }
  }

  previousProductPage(): void {
    if (
      !this.productsLoading() &&
      this.currentPage() > 0
    ) {
      this.loadProducts(this.currentPage() - 1);
    }
  }

  reset(): void {
    this.form = this.emptyForm();
    this.error.set('');
  }

  private validateForm(value: ProductForm): string {

    if (!value.name?.trim()) {
      return 'Please enter a product name.';
    }

    if (
      value.price == null ||
      !Number.isFinite(value.price) ||
      value.price < 0
    ) {
      return 'Please enter a valid price.';
    }

    if (
      value.quantity == null ||
      !Number.isInteger(value.quantity) ||
      value.quantity < 0
    ) {
      return 'Please enter a valid whole-number quantity.';
    }

    if ((value.description ?? '').length > 2000) {
      return 'Description cannot exceed 2000 characters.';
    }

    if ((value.category ?? '').length > 100) {
      return 'Category cannot exceed 100 characters.';
    }

    const imageUrl = (value.imageUrl ?? '').trim();

    if (imageUrl.length > 2000) {
      return 'Image URL cannot exceed 2000 characters.';
    }

    if (imageUrl && !/^https?:\/\/\S+$/i.test(imageUrl)) {
      return 'Image URL must start with http:// or https://.';
    }

    return '';
  }

  private toPayload(value: ProductForm): Product {
    return {
      name: value.name.trim(),
      price: value.price!,
      quantity: value.quantity!,
      description: value.description ?? '',
      category: value.category?.trim() || null,
      imageUrl: value.imageUrl?.trim() || null,
      active: true
    };
  }

  saveProduct(): void {

    if (this.creating) {
      return;
    }

    this.error.set('');
    this.message.set('');

    const validationError = this.validateForm(this.form);

    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.creating = true;

    this.productService.create(
      this.toPayload(this.form)
    ).subscribe({

      next: () => {
        this.creating = false;
        this.reset();
        this.loadProducts(0);
        this.message.set('Product created successfully.');
      },

      error: err => {
        this.creating = false;
        this.error.set(
          typeof err?.error === 'string'
            ? err.error
            : err?.error?.message || 'Unable to create product.'
        );
      }
    });
  }

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
      category: product.category ?? '',
      imageUrl: product.imageUrl ?? '',
      active: product.active ?? true
    };

    this.editError = '';
    this.message.set('');
    this.showEditPopup = true;
  }

  closeEditPopup(): void {

    if (this.saving) {
      return;
    }

    this.showEditPopup = false;
    this.editingId = null;
    this.editForm = this.emptyForm();
    this.editError = '';
  }

  updateProduct(): void {

    if (this.editingId === null || this.saving) {
      return;
    }

    this.editError = '';

    const validationError = this.validateForm(this.editForm);

    if (validationError) {
      this.editError = validationError;
      return;
    }

    this.saving = true;

    this.productService.update(
      this.editingId,
      this.toPayload(this.editForm)
    ).subscribe({

      next: () => {
        this.saving = false;
        this.closeEditPopup();
        this.loadProducts(this.currentPage());
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

  deleteProduct(id: number): void {

    if (!window.confirm(
      'Are you sure you want to delete this product?'
    )) {
      return;
    }

    this.error.set('');
    this.message.set('');

    this.productService.delete(id).subscribe({

      next: () => {
        const targetPage =
          this.products().length === 1 &&
          this.currentPage() > 0
            ? this.currentPage() - 1
            : this.currentPage();

        this.loadProducts(targetPage);
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

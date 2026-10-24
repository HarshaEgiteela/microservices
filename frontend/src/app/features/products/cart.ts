
import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { ProductService } from '../../core/services/product.service';
import { OrderService } from '../../core/services/order.service';

import {
  Address,
  AddressService
} from '../../core/services/address.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html'
})
export class CartComponent implements OnInit {

  readonly cart = inject(CartService);

  private readonly products = inject(ProductService);
  private readonly orders = inject(OrderService);
  private readonly addressService = inject(AddressService);
  private readonly router = inject(Router);

  readonly addresses = signal<Address[]>([]);
  readonly selectedAddressId = signal<number | null>(null);

  readonly message = signal('');
  readonly error = signal('');
  readonly placing = signal(false);
  readonly loadingAddresses = signal(false);
  readonly checkingStock = signal(false);

  readonly stockWarnings = signal<Record<number, string>>({});
  readonly stockLimits = signal<Record<number, number>>({});
  readonly unavailableItems = signal<number[]>([]);
  readonly stockValid = signal(false);

  private validationVersion = 0;

  ngOnInit(): void {
    void this.loadCartAndValidate();
    this.loadAddresses();
  }

  private async loadCartAndValidate(): Promise<void> {
    this.checkingStock.set(true);
    this.stockValid.set(false);

    try {
      await this.cart.loadAsync();
      await this.validateStock();
    } catch {
      this.error.set('Unable to load your cart. Please try again.');
    } finally {
      this.checkingStock.set(false);
    }
  }

  private async validateStock(): Promise<boolean> {
    const version = ++this.validationVersion;
    const items = [...this.cart.cartItems()];

    const warnings: Record<number, string> = {};
    const limits: Record<number, number> = {};
    const unavailable: number[] = [];

    let valid = true;

    for (const item of items) {
      try {
        const product = await firstValueFrom(
          this.products.getById(item.productId)
        );

        if (product.active === false) {
          warnings[item.itemId] =
            'Product no longer available — please remove this item.';

          unavailable.push(item.itemId);
          valid = false;
          continue;
        }

        if (product.quantity <= 0) {
          warnings[item.itemId] =
            'Out of stock — please remove this item.';

          unavailable.push(item.itemId);
          limits[item.itemId] = 0;
          valid = false;
          continue;
        }

        limits[item.itemId] = product.quantity;

        if (item.quantity > product.quantity) {
          warnings[item.itemId] =
            `Only ${product.quantity} unit${product.quantity === 1 ? '' : 's'} left — reduce quantity.`;

          valid = false;
        }

      } catch (err) {
        valid = false;

        if (
          err instanceof HttpErrorResponse &&
          err.status === 404
        ) {
          warnings[item.itemId] =
            'Product no longer available — please remove this item.';

          unavailable.push(item.itemId);
        } else {
          warnings[item.itemId] =
            'Unable to check availability. Please try again.';
        }
      }
    }

    if (version !== this.validationVersion) {
      return false;
    }

    this.stockWarnings.set(warnings);
    this.stockLimits.set(limits);
    this.unavailableItems.set(unavailable);
    this.stockValid.set(valid);

    return valid;
  }

  loadAddresses(): void {
    this.loadingAddresses.set(true);

    this.addressService.getAll().subscribe({
      next: addresses => {
        this.addresses.set(addresses);

        const selected = addresses.find(
          address => address.id === this.selectedAddressId()
        );

        const preferred =
          selected ??
          addresses.find(address => address.isDefault) ??
          addresses[0];

        this.selectedAddressId.set(preferred?.id ?? null);
        this.loadingAddresses.set(false);
      },

      error: () => {
        this.error.set('Unable to load delivery addresses.');
        this.loadingAddresses.set(false);
      }
    });
  }

  selectAddress(id: number): void {
    this.selectedAddressId.set(id);
    this.error.set('');
  }

  async increase(
    itemId: number,
    quantity: number
  ): Promise<void> {

    if (
      this.placing() ||
      this.checkingStock() ||
      this.unavailableItems().includes(itemId)
    ) {
      return;
    }

    const limit = this.stockLimits()[itemId];

    if (limit !== undefined && quantity >= limit) {
      this.stockWarnings.update(warnings => ({
        ...warnings,
        [itemId]: `Only ${limit} unit${limit === 1 ? '' : 's'} left.`
      }));
      return;
    }

    this.checkingStock.set(true);
    this.stockValid.set(false);

    try {
      await this.cart.updateAsync(itemId, quantity + 1);
      await this.validateStock();
    } catch {
      await this.validateStock();

      this.stockWarnings.update(warnings => ({
        ...warnings,
        [itemId]:
          'Could not increase quantity. Please try again.'
      }));
    } finally {
      this.checkingStock.set(false);
    }
  }

  async decrease(
    itemId: number,
    quantity: number
  ): Promise<void> {

    if (
      this.placing() ||
      this.checkingStock() ||
      this.unavailableItems().includes(itemId)
    ) {
      return;
    }

    this.checkingStock.set(true);
    this.stockValid.set(false);

    try {
      if (quantity <= 1) {
        await this.cart.removeAsync(itemId);
      } else {
        await this.cart.updateAsync(itemId, quantity - 1);
      }

      await this.validateStock();

    } catch (err) {
      await this.validateStock();

      const response =
        err instanceof HttpErrorResponse
          ? (
            typeof err.error === 'string'
              ? err.error
              : String(err.error?.message ?? '')
          )
          : '';

      const stockError =
        /insufficient stock|available:/i.test(response);

      this.stockWarnings.update(warnings => ({
        ...warnings,
        [itemId]: stockError
          ? 'The server rejected this reduction because the new quantity still exceeds stock. Please update the backend cart validation.'
          : 'Could not update quantity. Please try again.'
      }));

      this.stockValid.set(false);

    } finally {
      this.checkingStock.set(false);
    }
  }

  async remove(itemId: number): Promise<void> {
    if (this.placing() || this.checkingStock()) return;

    this.checkingStock.set(true);
    this.stockValid.set(false);

    try {
      await this.cart.removeAsync(itemId);
      await this.validateStock();
      this.error.set('');
    } catch {
      this.stockWarnings.update(warnings => ({
        ...warnings,
        [itemId]:
          'Unable to remove item. Please try again.'
      }));
    } finally {
      this.checkingStock.set(false);
    }
  }

  async checkout(): Promise<void> {
    if (
      this.placing() ||
      this.checkingStock() ||
      this.cart.loading() ||
      this.loadingAddresses()
    ) {
      return;
    }

    if (!this.cart.cartItems().length) {
      this.error.set('Your cart is empty.');
      return;
    }

    const addressId = this.selectedAddressId();

    if (addressId === null) {
      this.error.set('Please select a delivery address.');
      return;
    }

    this.error.set('');
    this.placing.set(true);
    this.checkingStock.set(true);

    try {
      const valid = await this.validateStock();

      if (!valid) {
        return;
      }

      await firstValueFrom(
        this.orders.checkout(addressId)
      );

      await this.cart.loadAsync();
      await this.router.navigate(['/orders']);

    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        await this.validateStock();

        if (Object.keys(this.stockWarnings()).length === 0) {
          this.error.set(
            typeof err.error === 'string'
              ? err.error
              : err.error?.message || 'Checkout failed.'
          );
        }
      } else {
        this.error.set('Checkout failed. Please try again.');
      }
    } finally {
      this.placing.set(false);
      this.checkingStock.set(false);
    }
  }
}

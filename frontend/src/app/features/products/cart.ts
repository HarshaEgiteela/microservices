
import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

import { Router, RouterLink } from '@angular/router';

import { CartService } from '../../core/services/cart.service';
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

  private readonly orders = inject(OrderService);
  private readonly addressService = inject(AddressService);
  private readonly router = inject(Router);

  readonly addresses = signal<Address[]>([]);
  readonly selectedAddressId = signal<number | null>(null);

  readonly message = signal('');
  readonly error = signal('');
  readonly placing = signal(false);
  readonly loadingAddresses = signal(false);

  ngOnInit(): void {
    this.cart.load();
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.loadingAddresses.set(true);
    this.error.set('');

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

  increase(itemId: number, quantity: number): void {
    if (this.placing()) return;

    this.cart.update(itemId, quantity + 1);
  }

  decrease(itemId: number, quantity: number): void {
    if (this.placing()) return;

    if (quantity <= 1) {
      this.cart.remove(itemId);
    } else {
      this.cart.update(itemId, quantity - 1);
    }
  }

  remove(itemId: number): void {
    if (this.placing()) return;

    this.cart.remove(itemId);
  }

  checkout(): void {
    if (
      this.placing() ||
      this.cart.loading() ||
      this.loadingAddresses()
    ) {
      return;
    }

    const addressId = this.selectedAddressId();

    if (!this.cart.cartItems().length) {
      this.error.set('Your cart is empty.');
      return;
    }

    if (addressId === null) {
      this.error.set('Please select a delivery address.');
      return;
    }

    this.error.set('');
    this.message.set('');
    this.placing.set(true);

    this.orders.checkout(addressId).subscribe({
      next: () => {
        this.placing.set(false);

        this.cart.load();

        this.message.set('Order placed successfully.');

        this.router.navigate(['/orders']);
      },

      error: err => {
        this.placing.set(false);

        this.error.set(
          typeof err.error === 'string'
            ? err.error
            : err.error?.message || 'Checkout failed.'
        );
      }
    });
  }
}

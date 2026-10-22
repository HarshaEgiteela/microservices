
import {
  Component, inject, OnInit, signal
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import {
  Address, AddressService
} from '../../core/services/address.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html'
})
export class CartComponent implements OnInit {
  readonly cart = inject(CartService);

  private orders = inject(OrderService);
  private addressService = inject(AddressService);
  private router = inject(Router);

  addresses = signal<Address[]>([]);
  selectedAddressId = signal<number | null>(null);

  message = signal('');
  error = signal('');
  placing = signal(false);
  loadingAddresses = signal(false);

  ngOnInit(): void {
    this.cart.load();
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.loadingAddresses.set(true);

    this.addressService.getAll().subscribe({
      next: addresses => {
        this.addresses.set(addresses);

        const selected = addresses.find(
          a => a.id === this.selectedAddressId()
        );

        const preferred = selected ??
          addresses.find(a => a.isDefault) ??
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
  }

  update(itemId: number, value: string): void {
    this.cart.update(itemId, Number(value));
  }

  remove(itemId: number): void {
    this.cart.remove(itemId);
  }

  checkout(): void {
    if (this.placing() || this.cart.loading()) return;

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

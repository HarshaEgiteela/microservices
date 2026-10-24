
import {
  Component,
  inject,
  HostListener,
  ElementRef,
  ViewChild
} from '@angular/core';

import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar.html'
})
export class NavbarComponent {

  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  readonly router = inject(Router);

  // Reference to the account dropdown
  @ViewChild('accountMenu')
  accountMenu?: ElementRef<HTMLDetailsElement>;

  isAuthPage(): boolean {
    return this.router.url.startsWith('/login') ||
      this.router.url.startsWith('/register');
  }

  isHomePage(): boolean {
    return this.router.url === '/' ||
      this.router.url.startsWith('/?');
  }

  logout(): void {
    this.cart.reset();
    this.auth.logout();
    this.router.navigate(['/']);
  }

  openCart(): void {
    if (!this.auth.hasToken()) {
      this.router.navigate(['/login'], {
        queryParams: {
          message: 'Please sign in to view your cart.',
          returnUrl: '/cart'
        }
      });
      return;
    }

    this.router.navigate(['/cart']);
  }

  // Close menu when selecting a menu option
  closeMenu(event: Event): void {
    const element = event.currentTarget as HTMLElement;
    const details = element.closest('details');

    details?.removeAttribute('open');
  }

  // Close menu when clicking outside
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {

    const menu = this.accountMenu?.nativeElement;

    if (!menu) {
      return;
    }

    const clickedElement = event.target as Node;

    // If clicked outside the dropdown, close it
    if (!menu.contains(clickedElement)) {
      menu.open = false;
    }
  }

  // Close menu when Escape is pressed
  @HostListener('document:keydown.escape')
  onEscape(): void {
    const menu = this.accountMenu?.nativeElement;

    if (menu) {
      menu.open = false;
    }
  }
}

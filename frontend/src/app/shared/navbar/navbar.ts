import { Component, inject } from '@angular/core';
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

  isAuthPage(): boolean {
    return this.router.url.startsWith('/login') ||
      this.router.url.startsWith('/register');
  }

  isHomePage(): boolean {
    return this.router.url === '/' || this.router.url.startsWith('/?');
  }

  logout(): void {
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

  closeMenu(event: Event): void {
    const element = event.currentTarget as HTMLElement;
    const details = element.closest('details');
    details?.removeAttribute('open');
  }
}

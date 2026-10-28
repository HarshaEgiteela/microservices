
import {
  Component,
  inject,
  OnInit
} from '@angular/core';

import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from './shared/navbar/navbar';
import { ToastComponent } from './shared/toast/toast';

import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    NavbarComponent,
    ToastComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  private readonly auth = inject(AuthService);

  ngOnInit(): void {

    // Restore profile after a browser refresh.
    if (this.auth.hasToken()) {

      this.auth.loadProfile().subscribe({
        next: () => {
          // Profile and role are now available in memory.
        },

        error: () => {
          // The interceptor handles 401 responses.
          // Other failures leave the session intact.
        }
      });
    }
  }
}

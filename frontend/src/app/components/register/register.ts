import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {

  user = {
    name: '',
    email: '',
    password: ''
  };

  message = '';
  isRegistering = false;

  constructor(private userService: UserService) {}

  register(): void {

    if (this.isRegistering) {
      return;
    }

    this.isRegistering = true;

    console.log('Sending registration:', this.user);

    this.userService.register(this.user)
      .pipe(
        finalize(() => {
          console.log('Registration request finished');
          this.isRegistering = false;
        })
      )
      .subscribe({

        next: (response) => {

          console.log('Registration successful:', response);

          this.message = 'Registration successful';

          // Clear form
          this.user = {
            name: '',
            email: '',
            password: ''
          };

          // Refresh users list
          this.userService.notifyUserRegistered();
        },

        error: (error) => {

          console.error('Registration failed:', error);

          this.message = 'Registration failed';
        }

      });
  }
}

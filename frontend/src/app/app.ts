import { Component, signal } from '@angular/core';
import { Users } from './components/users/users';
import { Register } from './components/register/register';

@Component({
  selector: 'app-root',
  imports: [Users, Register],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}

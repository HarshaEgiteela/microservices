import {
  Component,
  signal
} from '@angular/core';

@Component({
  selector: 'app-toast',
  standalone: true,
  templateUrl: './toast.html'
})
export class ToastComponent {

  readonly message =
    signal('');

  readonly type =
    signal<'success' | 'error'>('success');

  show(
    message: string,
    type: 'success' | 'error' = 'success'
  ): void {

    this.message.set(message);
    this.type.set(type);

    setTimeout(
      () => this.message.set(''),
      3000
    );
  }
}

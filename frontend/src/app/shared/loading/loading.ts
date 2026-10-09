import {
  Component,
  Input
} from '@angular/core';

@Component({
  selector: 'app-loading',
  standalone: true,
  templateUrl: './loading.html'
})
export class LoadingComponent {

  @Input()
  label = 'Loading...';
}


import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Address,
  AddressRequest,
  AddressService
} from '../../core/services/address.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './profile.html'
})
export class ProfileComponent implements OnInit {
  private readonly service = inject(AddressService);

  addresses = signal<Address[]>([]);
  loading = signal(false);
  saving = signal(false);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  error = signal('');
  message = signal('');

  form: AddressRequest = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  private emptyForm(): AddressRequest {
    return {
      label: 'Home',
      recipientName: '',
      phone: '',
      addressLine1: '',
      addressLine2: '',
      city: '',
      state: '',
      postalCode: '',
      country: 'India'
    };
  }

  private getErrorMessage(
    err: HttpErrorResponse,
    fallback: string
  ): string {
    console.error('Address API error:', err);

    if (typeof err.error === 'string' && err.error.trim()) {
      return err.error;
    }

    if (err.error?.message) {
      return err.error.message;
    }

    return `${fallback} (HTTP ${err.status || 'Network Error'})`;
  }

  load(): void {
    this.loading.set(true);

    this.service.getAll().subscribe({
      next: data => {
        this.addresses.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          this.getErrorMessage(err, 'Could not load addresses.')
        );
        this.loading.set(false);
      }
    });
  }

  addNew(): void {
    this.editingId.set(null);
    this.form = this.emptyForm();
    this.error.set('');
    this.message.set('');
    this.showForm.set(true);
  }

  edit(a: Address): void {
    this.editingId.set(a.id);

    this.form = {
      label: a.label,
      recipientName: a.recipientName,
      phone: a.phone,
      addressLine1: a.addressLine1,
      addressLine2: a.addressLine2 ?? '',
      city: a.city,
      state: a.state,
      postalCode: a.postalCode,
      country: a.country
    };

    this.error.set('');
    this.message.set('');
    this.showForm.set(true);
  }

  save(): void {
    if (this.saving()) return;

    this.saving.set(true);
    this.error.set('');
    this.message.set('');

    const id = this.editingId();

    const request = id === null
      ? this.service.add(this.form)
      : this.service.update(id, this.form);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.message.set('Address saved successfully.');
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.error.set(
          this.getErrorMessage(err, 'Failed to save address.')
        );
      }
    });
  }

  setDefault(id: number): void {
    this.error.set('');
    this.message.set('');

    this.service.setDefault(id).subscribe({
      next: () => {
        this.message.set('Default address updated successfully.');
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          this.getErrorMessage(err, 'Failed to set default address.')
        );
      }
    });
  }

  remove(id: number): void {
    if (!confirm('Delete this address?')) return;

    this.error.set('');
    this.message.set('');

    this.service.delete(id).subscribe({
      next: () => {
        this.message.set('Address deleted successfully.');
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          this.getErrorMessage(err, 'Failed to delete address.')
        );
      }
    });
  }

  cancel(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.error.set('');
  }
}

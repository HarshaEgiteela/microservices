
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';

export interface AddressRequest {
  label: string;
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Address extends AddressRequest {
  id: number;
  isDefault: boolean;
}

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);

  private readonly url = `${API_BASE_URL}/users/me/addresses`;

  getAll(): Observable<Address[]> {
    return this.http.get<Address[]>(this.url);
  }

  add(data: AddressRequest): Observable<Address> {
    return this.http.post<Address>(this.url, data);
  }

  update(id: number, data: AddressRequest): Observable<Address> {
    return this.http.put<Address>(
      `${this.url}/${id}`,
      data
    );
  }

  setDefault(id: number): Observable<Address> {
    return this.http.patch<Address>(
      `${this.url}/${id}/default`,
      {}
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.url}/${id}`
    );
  }
}

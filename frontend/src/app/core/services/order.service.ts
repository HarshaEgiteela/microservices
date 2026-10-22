
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Order, OrderStatus } from '../models/app.models';

@Injectable({ providedIn: 'root' })
export class OrderService {

  constructor(private readonly http: HttpClient) {}

  // Admin: get all orders
  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(`${API_BASE_URL}/orders`);
  }

  // Logged-in customer: get only their orders
  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${API_BASE_URL}/orders/my`);
  }

  // Place order using backend cart and selected address
  checkout(addressId: number): Observable<unknown> {
    return this.http.post(
      `${API_BASE_URL}/orders/checkout`,
      { addressId }
    );
  }

  // Admin: update order status
  updateStatus(
    orderId: number,
    status: OrderStatus
  ): Observable<Order> {
    return this.http.put<Order>(
      `${API_BASE_URL}/orders/${orderId}/status`,
      null,
      { params: { status } }
    );
  }
}

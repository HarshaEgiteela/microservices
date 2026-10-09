import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Order, OrderStatus } from '../models/app.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(`${API_BASE_URL}/orders`);
  }

  placeOrder(
    userId: number,
    productId: number,
    quantity: number
  ): Observable<unknown> {
    return this.http.post(
      `${API_BASE_URL}/orders`,
      { userId, productId, quantity }
    );
  }

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

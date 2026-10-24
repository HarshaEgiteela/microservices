
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { Order, OrderStatus } from '../models/app.models';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(
    private readonly http: HttpClient
  ) {}

  // ==========================================
  // ADMIN: GET ALL ORDERS
  // ==========================================

  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${API_BASE_URL}/orders`
    );
  }

  // ==========================================
  // CUSTOMER: GET MY ORDERS
  // ==========================================

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${API_BASE_URL}/orders/my`
    );
  }

  // ==========================================
  // CUSTOMER: CHECKOUT
  // ==========================================

  checkout(addressId: number): Observable<unknown> {
    return this.http.post(
      `${API_BASE_URL}/orders/checkout`,
      { addressId }
    );
  }

  // ==========================================
  // ADMIN: UPDATE ORDER STATUS
  // ==========================================

  updateStatus(
    orderId: number,
    status: OrderStatus
  ): Observable<Order> {

    return this.http.put<Order>(
      `${API_BASE_URL}/orders/${orderId}/status`,
      null,
      {
        params: { status }
      }
    );
  }

  // ==========================================
  // CUSTOMER: CANCEL MY ORDER
  // ==========================================

  cancelMyOrder(orderId: number): Observable<Order> {

    return this.http.put<Order>(
      `${API_BASE_URL}/orders/my/${orderId}/cancel`,
      null
    );
  }

  // ==========================================
  // ADMIN: CANCEL ANY ORDER
  // ==========================================

  cancelOrder(orderId: number): Observable<Order> {

    return this.http.put<Order>(
      `${API_BASE_URL}/orders/${orderId}/cancel`,
      null
    );
  }
}

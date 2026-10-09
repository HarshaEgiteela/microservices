import { Injectable } from '@angular/core';
import {
  HttpClient
} from '@angular/common/http';

import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { Product } from '../models/app.models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(
    private readonly http: HttpClient
  ) {}

  getAll(): Observable<Product[]> {

    return this.http.get<Product[]>(
      `${API_BASE_URL}/products`
    );
  }

  getById(id: number): Observable<Product> {

    return this.http.get<Product>(
      `${API_BASE_URL}/products/${id}`
    );
  }

  create(
    product: Product
  ): Observable<Product> {

    return this.http.post<Product>(
      `${API_BASE_URL}/products`,
      product
    );
  }

  update(
    id: number,
    product: Product
  ): Observable<Product> {

    return this.http.put<Product>(
      `${API_BASE_URL}/products/${id}`,
      product
    );
  }

  delete(id: number): Observable<void> {

    return this.http.delete<void>(
      `${API_BASE_URL}/products/${id}`
    );
  }
}

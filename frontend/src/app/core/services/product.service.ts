
import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { Product } from '../models/app.models';

// Spring Boot pagination response
export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Product search and filter options
export interface ProductFilters {
  search?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  sort?: 'name_asc' | 'name_desc' | 'price_asc' | 'price_desc';
  page?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(
    private readonly http: HttpClient
  ) {}

  // ========================================
  // PUBLIC: SEARCH, FILTER, SORT, PAGINATE
  // ========================================

  getProducts(
    filters: ProductFilters = {}
  ): Observable<ProductPage> {

    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20))
      .set('sort', filters.sort ?? 'name_asc');

    if (filters.search?.trim()) {
      params = params.set(
        'search',
        filters.search.trim()
      );
    }

    if (filters.category?.trim()) {
      params = params.set(
        'category',
        filters.category.trim()
      );
    }

    if (filters.minPrice != null) {
      params = params.set(
        'minPrice',
        String(filters.minPrice)
      );
    }

    if (filters.maxPrice != null) {
      params = params.set(
        'maxPrice',
        String(filters.maxPrice)
      );
    }

    return this.http.get<ProductPage>(
      `${API_BASE_URL}/products`,
      { params }
    );
  }

  // ========================================
  // COMPATIBILITY: EXISTING getAll() CALLS
  // ========================================

  // Temporary compatibility for existing components.
  // Returns only the first 100 products.
  // Does not retrieve the entire catalog.
  getAll(): Observable<Product[]> {

    return this.getProducts({
      page: 0,
      size: 100
    }).pipe(
      map(response => response.content)
    );
  }

  // ========================================
  // PUBLIC: GET PRODUCT BY ID
  // ========================================

  getById(id: number): Observable<Product> {

    return this.http.get<Product>(
      `${API_BASE_URL}/products/${id}`
    );
  }

  // ========================================
  // ADMIN: CREATE PRODUCT
  // ========================================

  create(product: Product): Observable<Product> {

    return this.http.post<Product>(
      `${API_BASE_URL}/products`,
      product
    );
  }

  // ========================================
  // ADMIN: UPDATE PRODUCT
  // ========================================

  update(
    id: number,
    product: Product
  ): Observable<Product> {

    return this.http.put<Product>(
      `${API_BASE_URL}/products/${id}`,
      product
    );
  }

  // ========================================
  // ADMIN: SOFT DELETE PRODUCT
  // ========================================

  delete(id: number): Observable<void> {

    return this.http.delete<void>(
      `${API_BASE_URL}/products/${id}`
    );
  }
}

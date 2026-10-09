export interface User {
  id?: number;
  name: string;
  email: string;
  role?: string;
  enabled?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token?: string;
  jwt?: string;
  role?: string;
  userId?: number;
  id?: number;
  user?: User;
}

export interface Product {
  id?: number;
  name: string;
  price: number;
  quantity: number;
  description?: string;
}

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export interface Order {
  id?: number;
  userId: number;
  productId: number;
  quantity: number;
  status: OrderStatus;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

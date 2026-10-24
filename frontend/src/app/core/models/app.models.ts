
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
  active?: boolean;
}

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCEL_PENDING'
  | 'CANCELLED';

export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderDeliveryAddress {
  recipientName: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Order {
  id: number;
  userId: number;
  status: OrderStatus;
  createdAt: string;
  items: OrderItem[];
  totalAmount: number;
  deliveryAddress: OrderDeliveryAddress | null;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

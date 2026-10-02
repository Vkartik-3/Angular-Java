export enum OrderStatus {
  CREATED            = 'CREATED',
  PAID               = 'PAID',
  INVENTORY_APPROVED = 'INVENTORY_APPROVED',
  PAYMENT_FAILED     = 'PAYMENT_FAILED',
  INVENTORY_REJECTED = 'INVENTORY_REJECTED',
  CANCELLED          = 'CANCELLED',
}

export type HistoryStatus = OrderStatus | 'PAYMENT_ROLLED_BACK';

export interface OrderItemRequest {
  productId: string;
  quantity: number;
  price: number;
  productName?: string;
  imageUrl?: string;
}

export interface CreateOrderRequest {
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  productId: string;
  quantity: number;
  price: number;
  productName?: string | null;
  imageUrl?: string | null;
}

export interface StatusHistoryEntry {
  status: HistoryStatus;
  occurredAt: string;
}

export interface OrderResponse {
  id: string;
  username: string;
  status: OrderStatus;
  items: OrderItemResponse[];
  total: number;
  history: StatusHistoryEntry[];
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  code?: string;
  message?: string;
  data?: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Member {
  id: number;
  email: string;
  name: string;
  phone?: string;
  address?: string;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginResponse {
  token: TokenResponse;
  member: Member;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  categoryId?: number;
  categoryName?: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  sortOrder: number;
  parentId?: number;
  parentName?: string;
  children?: Category[];
}

export interface OrderItem {
  orderItemId: number;
  productId: number;
  snapshotProductName: string;
  snapshotPrice: number;
  quantity: number;
  subtotal: number;
  status: string;
}

export interface Order {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  shippingAddress: string;
  receiverName: string;
  receiverPhone: string;
  items: OrderItem[];
  createdAt: string;
}

export interface CartItemData {
  productId: number;
  name: string;
  price: number;
  imageUrl?: string;
  quantity: number;
  stockQuantity: number;
}

export interface KpiSummary {
  totalRevenue: number;
  totalOrders: number;
  paidOrders: number;
  newMembers: number;
  totalVisitors: number;
  conversionRate: number;
  onlineUsers: number;
}

export interface DailySales {
  date: string;
  revenue: number;
  orderCount: number;
}

export interface CategorySales {
  categoryId: number;
  categoryName: string;
  revenue: number;
  quantity: number;
}

export interface DashboardData {
  kpi: KpiSummary;
  dailySales: DailySales[];
  categorySales: CategorySales[];
}

export interface PaymentResponse {
  paymentId: number;
  orderId: number;
  idempotencyKey: string;
  amount: number;
  paymentMethod: string;
  status: string;
  createdAt: string;
}

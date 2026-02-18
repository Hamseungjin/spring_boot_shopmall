export function formatPrice(value: number): string {
  return new Intl.NumberFormat('ko-KR').format(value) + '원';
}

export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
}

export function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('ko-KR');
}

export function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 10)}`;
}

export function downloadCsv(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

const STATUS_MAP: Record<string, string> = {
  PENDING_PAYMENT: '결제대기',
  PAID: '결제완료',
  PREPARING: '상품준비중',
  SHIPPED: '배송중',
  DELIVERED: '배송완료',
  CANCELLED: '취소됨',
  REFUND_REQUESTED: '환불요청',
  REFUNDED: '환불완료',
};

export function orderStatusLabel(status: string): string {
  return STATUS_MAP[status] ?? status;
}

const STATUS_COLOR: Record<string, string> = {
  PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
  PAID: 'bg-blue-100 text-blue-800',
  PREPARING: 'bg-indigo-100 text-indigo-800',
  SHIPPED: 'bg-purple-100 text-purple-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  REFUND_REQUESTED: 'bg-orange-100 text-orange-800',
  REFUNDED: 'bg-gray-100 text-gray-800',
};

export function orderStatusColor(status: string): string {
  return STATUS_COLOR[status] ?? 'bg-gray-100 text-gray-800';
}

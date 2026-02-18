import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Package, Loader2, ChevronDown, ChevronUp } from 'lucide-react';
import { orderApi } from '@/api/orders';
import { formatPrice, formatDateTime, orderStatusLabel, orderStatusColor } from '@/lib/utils';
import Button from '@/components/common/Button';
import type { Order } from '@/types';

export default function OrderListPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['myOrders', page],
    queryFn: () => orderApi.getMyOrders(page, 10).then((r) => r.data.data!),
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => orderApi.cancel(orderId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['myOrders'] }),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  const orders = data?.content ?? [];

  if (orders.length === 0) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center text-gray-400">
        <Package className="h-16 w-16" />
        <p className="mt-4 text-lg">주문 내역이 없습니다</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">주문 내역</h1>

      <div className="space-y-4">
        {orders.map((order) => (
          <OrderCard
            key={order.orderId}
            order={order}
            onCancel={() => cancelMutation.mutate(order.orderId)}
            cancelling={cancelMutation.isPending}
          />
        ))}
      </div>

      {data && !data.last && (
        <div className="flex justify-center">
          <Button variant="outline" onClick={() => setPage((p) => p + 1)}>
            더 보기
          </Button>
        </div>
      )}
    </div>
  );
}

function OrderCard({
  order,
  onCancel,
  cancelling,
}: {
  order: Order;
  onCancel: () => void;
  cancelling: boolean;
}) {
  const [open, setOpen] = useState(false);
  const cancellable = ['PENDING_PAYMENT', 'PAID', 'PREPARING'].includes(order.status);

  return (
    <div className="rounded-xl border border-gray-200 bg-white">
      <div
        className="flex cursor-pointer items-center justify-between p-4"
        onClick={() => setOpen(!open)}
      >
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-gray-900">{order.orderNumber}</span>
            <span
              className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${orderStatusColor(order.status)}`}
            >
              {orderStatusLabel(order.status)}
            </span>
          </div>
          <p className="text-xs text-gray-400">{formatDateTime(order.createdAt)}</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm font-bold text-primary-600">{formatPrice(order.totalAmount)}</span>
          {open ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
        </div>
      </div>

      {open && (
        <div className="border-t border-gray-100 px-4 pb-4 pt-2">
          <div className="divide-y">
            {order.items.map((item) => (
              <div key={item.orderItemId} className="flex items-center justify-between py-2.5">
                <div>
                  <p className="text-sm font-medium">{item.snapshotProductName}</p>
                  <p className="text-xs text-gray-400">
                    {formatPrice(item.snapshotPrice)} × {item.quantity}
                    <span className={`ml-2 rounded px-1.5 py-0.5 text-xs ${orderStatusColor(item.status)}`}>
                      {orderStatusLabel(item.status)}
                    </span>
                  </p>
                </div>
                <span className="text-sm font-bold">{formatPrice(item.subtotal)}</span>
              </div>
            ))}
          </div>

          <div className="mt-3 flex items-center justify-between border-t pt-3">
            <p className="text-xs text-gray-400">
              {order.receiverName} · {order.shippingAddress}
            </p>
            {cancellable && (
              <Button variant="danger" size="sm" onClick={onCancel} loading={cancelling}>
                주문 취소
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

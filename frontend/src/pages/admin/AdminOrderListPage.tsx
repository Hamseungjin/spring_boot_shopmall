import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, Package, Eye } from 'lucide-react';
import { orderApi } from '@/api/orders';
import {
  formatPrice,
  formatDateTime,
  orderStatusLabel,
  orderStatusColor,
} from '@/lib/utils';
import Button from '@/components/common/Button';
import type { Order } from '@/types';

const ALL_STATUSES = [
  'PENDING_PAYMENT',
  'PAID',
  'PREPARING',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUND_REQUESTED',
  'REFUNDED',
];

export default function AdminOrderListPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['adminOrders', page],
    queryFn: () => orderApi.getMyOrders(page, 20).then((r) => r.data.data!),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ orderId, status }: { orderId: number; status: string }) =>
      orderApi.updateStatus(orderId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminOrders'] }),
  });

  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">주문 관리</h1>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        </div>
      ) : orders.length === 0 ? (
        <div className="flex flex-col items-center py-20 text-gray-400">
          <Package className="h-16 w-16" />
          <p className="mt-4 text-lg">주문이 없습니다</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-4 py-3 font-medium text-gray-500">주문번호</th>
                <th className="px-4 py-3 font-medium text-gray-500">주문자</th>
                <th className="px-4 py-3 font-medium text-gray-500">금액</th>
                <th className="px-4 py-3 font-medium text-gray-500">상태</th>
                <th className="px-4 py-3 font-medium text-gray-500">상태 변경</th>
                <th className="px-4 py-3 font-medium text-gray-500">일시</th>
                <th className="px-4 py-3 font-medium text-gray-500">상세</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {orders.map((order: Order) => (
                <tr key={order.orderId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {order.orderNumber}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{order.receiverName}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {formatPrice(order.totalAmount)}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${orderStatusColor(order.status)}`}
                    >
                      {orderStatusLabel(order.status)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <select
                      value={order.status}
                      onChange={(e) =>
                        updateStatusMutation.mutate({
                          orderId: order.orderId,
                          status: e.target.value,
                        })
                      }
                      disabled={updateStatusMutation.isPending}
                      className="rounded-lg border border-gray-300 px-2 py-1 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      {ALL_STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {orderStatusLabel(s)}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">
                    {formatDateTime(order.createdAt)}
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/orders/${order.orderId}`}
                      className="rounded p-1.5 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                    >
                      <Eye className="h-4 w-4" />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            이전
          </Button>
          <span className="text-sm text-gray-500">
            {page + 1} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  );
}

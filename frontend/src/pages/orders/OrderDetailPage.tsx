import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  Loader2,
  Package,
  Clock,
  MapPin,
  Phone,
  User,
} from 'lucide-react';
import { orderApi } from '@/api/orders';
import {
  formatPrice,
  formatDateTime,
  orderStatusLabel,
  orderStatusColor,
} from '@/lib/utils';
import Button from '@/components/common/Button';
import type { OrderItem, OrderStatusHistory } from '@/types';

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const orderId = Number(id);

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => orderApi.getById(orderId).then((r) => r.data.data!),
    enabled: !!id,
  });

  const { data: history } = useQuery({
    queryKey: ['orderHistory', orderId],
    queryFn: () => orderApi.getHistory(orderId).then((r) => r.data.data ?? []),
    enabled: !!id,
  });

  const cancelMutation = useMutation({
    mutationFn: (reason: string) => orderApi.cancel(orderId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', orderId] });
      queryClient.invalidateQueries({ queryKey: ['orderHistory', orderId] });
    },
  });

  const cancelItemMutation = useMutation({
    mutationFn: ({ itemId, reason }: { itemId: number; reason: string }) =>
      orderApi.cancelItem(orderId, itemId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', orderId] });
      queryClient.invalidateQueries({ queryKey: ['orderHistory', orderId] });
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!order) {
    return <p className="py-20 text-center text-gray-500">주문을 찾을 수 없습니다.</p>;
  }

  const cancellable = ['PENDING_PAYMENT', 'PAID', 'PREPARING'].includes(order.status);

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" /> 뒤로가기
      </button>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">주문 상세</h1>
          <p className="mt-1 text-sm text-gray-500">주문번호: {order.orderNumber}</p>
        </div>
        <span
          className={`inline-block self-start rounded-full px-4 py-1.5 text-sm font-medium ${orderStatusColor(order.status)}`}
        >
          {orderStatusLabel(order.status)}
        </span>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {/* 주문 아이템 */}
          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="text-lg font-bold text-gray-900">주문 상품</h3>
            <div className="mt-4 divide-y">
              {order.items.map((item) => (
                <OrderItemRow
                  key={item.orderItemId}
                  item={item}
                  canCancel={cancellable && !['CANCELLED', 'REFUNDED'].includes(item.status)}
                  onCancel={(reason) =>
                    cancelItemMutation.mutate({ itemId: item.orderItemId, reason })
                  }
                  cancelling={cancelItemMutation.isPending}
                />
              ))}
            </div>
            <div className="mt-4 flex items-center justify-between border-t pt-4">
              <span className="font-medium text-gray-700">총 결제 금액</span>
              <span className="text-xl font-bold text-primary-600">
                {formatPrice(order.totalAmount)}
              </span>
            </div>
          </div>

          {/* 상태 이력 */}
          {history && history.length > 0 && (
            <div className="rounded-xl border border-gray-200 bg-white p-6">
              <h3 className="text-lg font-bold text-gray-900">주문 이력</h3>
              <div className="mt-4 space-y-3">
                {history.map((h: OrderStatusHistory) => (
                  <div key={h.id} className="flex items-start gap-3">
                    <Clock className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
                    <div className="text-sm">
                      <p className="text-gray-700">
                        <span className={`rounded px-1.5 py-0.5 text-xs ${orderStatusColor(h.previousStatus)}`}>
                          {orderStatusLabel(h.previousStatus)}
                        </span>
                        <span className="mx-1.5 text-gray-400">→</span>
                        <span className={`rounded px-1.5 py-0.5 text-xs ${orderStatusColor(h.newStatus)}`}>
                          {orderStatusLabel(h.newStatus)}
                        </span>
                      </p>
                      {h.reason && (
                        <p className="mt-0.5 text-xs text-gray-500">사유: {h.reason}</p>
                      )}
                      <p className="mt-0.5 text-xs text-gray-400">{formatDateTime(h.createdAt)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 우측 사이드 */}
        <div className="space-y-4">
          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="text-lg font-bold text-gray-900">배송 정보</h3>
            <div className="mt-4 space-y-3 text-sm">
              <div className="flex items-center gap-2 text-gray-600">
                <User className="h-4 w-4 text-gray-400" />
                {order.receiverName}
              </div>
              <div className="flex items-center gap-2 text-gray-600">
                <Phone className="h-4 w-4 text-gray-400" />
                {order.receiverPhone}
              </div>
              <div className="flex items-start gap-2 text-gray-600">
                <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
                {order.shippingAddress}
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="text-lg font-bold text-gray-900">주문 정보</h3>
            <div className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">주문일시</span>
                <span className="text-gray-700">{formatDateTime(order.createdAt)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">상품 수</span>
                <span className="text-gray-700">{order.items.length}종</span>
              </div>
            </div>
          </div>

          {cancellable && (
            <CancelOrderButton
              onCancel={(reason) => cancelMutation.mutate(reason)}
              cancelling={cancelMutation.isPending}
            />
          )}
        </div>
      </div>
    </div>
  );
}

function OrderItemRow({
  item,
  canCancel,
  onCancel,
  cancelling,
}: {
  item: OrderItem;
  canCancel: boolean;
  onCancel: (reason: string) => void;
  cancelling: boolean;
}) {
  const [showCancel, setShowCancel] = useState(false);
  const [reason, setReason] = useState('');

  return (
    <div className="py-3">
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <Package className="h-4 w-4 text-gray-400" />
            <span className="text-sm font-medium text-gray-900">{item.snapshotProductName}</span>
          </div>
          <p className="ml-6 text-xs text-gray-400">
            {formatPrice(item.snapshotPrice)} × {item.quantity}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-medium ${orderStatusColor(item.status)}`}
          >
            {orderStatusLabel(item.status)}
          </span>
          <span className="text-sm font-bold text-gray-900">{formatPrice(item.subtotal)}</span>
        </div>
      </div>

      {canCancel && (
        <div className="ml-6 mt-2">
          {!showCancel ? (
            <button
              onClick={() => setShowCancel(true)}
              className="text-xs text-red-500 hover:text-red-700"
            >
              이 상품 취소
            </button>
          ) : (
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="취소 사유 (선택)"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="flex-1 rounded-lg border border-gray-300 px-2 py-1 text-xs"
              />
              <Button
                variant="danger"
                size="sm"
                onClick={() => {
                  onCancel(reason);
                  setShowCancel(false);
                  setReason('');
                }}
                loading={cancelling}
              >
                확인
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setShowCancel(false);
                  setReason('');
                }}
              >
                닫기
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function CancelOrderButton({
  onCancel,
  cancelling,
}: {
  onCancel: (reason: string) => void;
  cancelling: boolean;
}) {
  const [show, setShow] = useState(false);
  const [reason, setReason] = useState('');

  if (!show) {
    return (
      <Button variant="danger" className="w-full" onClick={() => setShow(true)}>
        전체 주문 취소
      </Button>
    );
  }

  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p className="mb-2 text-sm font-medium text-red-700">주문을 취소하시겠습니까?</p>
      <input
        type="text"
        placeholder="취소 사유 (선택)"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        className="mb-3 w-full rounded-lg border border-red-300 px-3 py-2 text-sm"
      />
      <div className="flex gap-2">
        <Button
          variant="danger"
          className="flex-1"
          onClick={() => onCancel(reason)}
          loading={cancelling}
        >
          취소 확인
        </Button>
        <Button variant="ghost" className="flex-1" onClick={() => setShow(false)}>
          돌아가기
        </Button>
      </div>
    </div>
  );
}

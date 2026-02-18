import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { CreditCard, Loader2 } from 'lucide-react';
import { orderApi } from '@/api/orders';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { formatPrice, generateIdempotencyKey } from '@/lib/utils';
import Button from '@/components/common/Button';
import Input from '@/components/common/Input';

const schema = z.object({
  receiverName: z.string().min(1, '수령인 이름을 입력해주세요.'),
  receiverPhone: z.string().min(1, '전화번호를 입력해주세요.'),
  shippingAddress: z.string().min(1, '배송 주소를 입력해주세요.'),
});

type FormData = z.infer<typeof schema>;

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { items, totalPrice, clearCart } = useCartStore();
  const member = useAuthStore((s) => s.member);
  const [step, setStep] = useState<'info' | 'paying' | 'done'>('info');
  const [error, setError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      receiverName: member?.name ?? '',
      receiverPhone: member?.phone ?? '',
      shippingAddress: member?.address ?? '',
    },
  });

  const orderMutation = useMutation({
    mutationFn: async (data: FormData) => {
      const orderRes = await orderApi.create({
        ...data,
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
      });
      const order = orderRes.data.data!;

      const payRes = await orderApi.pay(order.orderId, generateIdempotencyKey(), 'CARD');
      return { order, payment: payRes.data.data! };
    },
    onSuccess: ({ order }) => {
      setStep('done');
      clearCart();
      setTimeout(() => navigate(`/orders`), 2000);
    },
    onError: (err: any) => {
      setStep('info');
      setError(err.response?.data?.message || '주문 처리 중 오류가 발생했습니다.');
    },
  });

  if (items.length === 0 && step !== 'done') {
    navigate('/cart');
    return null;
  }

  if (step === 'done') {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
        <div className="rounded-full bg-green-100 p-4">
          <CreditCard className="h-10 w-10 text-green-600" />
        </div>
        <h2 className="mt-4 text-2xl font-bold text-gray-900">주문이 완료되었습니다!</h2>
        <p className="mt-2 text-gray-500">주문 내역 페이지로 이동합니다...</p>
      </div>
    );
  }

  const onSubmit = (data: FormData) => {
    setStep('paying');
    setError('');
    orderMutation.mutate(data);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">주문/결제</h1>

      {error && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</div>}

      <div className="grid gap-6 lg:grid-cols-3">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="text-lg font-bold text-gray-900">배송 정보</h3>
            <div className="mt-4 space-y-4">
              <Input label="수령인" error={errors.receiverName?.message} {...register('receiverName')} />
              <Input label="전화번호" error={errors.receiverPhone?.message} {...register('receiverPhone')} />
              <Input label="배송 주소" error={errors.shippingAddress?.message} {...register('shippingAddress')} />
            </div>
          </div>

          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="text-lg font-bold text-gray-900">주문 상품</h3>
            <div className="mt-4 divide-y">
              {items.map((item) => (
                <div key={item.productId} className="flex items-center justify-between py-3">
                  <div>
                    <p className="text-sm font-medium">{item.name}</p>
                    <p className="text-xs text-gray-400">{formatPrice(item.price)} × {item.quantity}</p>
                  </div>
                  <span className="text-sm font-bold">{formatPrice(item.price * item.quantity)}</span>
                </div>
              ))}
            </div>
          </div>

          <Button type="submit" className="w-full" size="lg" disabled={step === 'paying'}>
            {step === 'paying' ? (
              <><Loader2 className="mr-2 h-5 w-5 animate-spin" /> 결제 처리중...</>
            ) : (
              <>{formatPrice(totalPrice())} 결제하기</>
            )}
          </Button>
        </form>

        <div className="rounded-xl border border-gray-200 bg-white p-6 lg:sticky lg:top-24 lg:self-start">
          <h3 className="text-lg font-bold text-gray-900">결제 금액</h3>
          <div className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">상품 금액</span>
              <span>{formatPrice(totalPrice())}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">배송비</span>
              <span className="text-green-600">무료</span>
            </div>
            <div className="my-3 border-t" />
            <div className="flex justify-between text-lg font-bold">
              <span>총 결제금액</span>
              <span className="text-primary-600">{formatPrice(totalPrice())}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

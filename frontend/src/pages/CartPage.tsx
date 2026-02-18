import { Link, useNavigate } from 'react-router-dom';
import { Trash2, Minus, Plus, ShoppingBag } from 'lucide-react';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { formatPrice } from '@/lib/utils';
import Button from '@/components/common/Button';

export default function CartPage() {
  const { items, removeItem, updateQuantity, totalPrice } = useCartStore();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center text-center">
        <ShoppingBag className="h-16 w-16 text-gray-300" />
        <h2 className="mt-4 text-xl font-bold text-gray-900">장바구니가 비어있습니다</h2>
        <p className="mt-2 text-sm text-gray-500">마음에 드는 상품을 담아보세요.</p>
        <Link to="/products" className="mt-6">
          <Button>상품 보러가기</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">장바구니</h1>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {items.map((item) => (
            <div
              key={item.productId}
              className="flex gap-4 rounded-xl border border-gray-200 bg-white p-4"
            >
              <Link
                to={`/products/${item.productId}`}
                className="h-24 w-24 shrink-0 overflow-hidden rounded-lg bg-gray-100"
              >
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full items-center justify-center text-gray-300">
                    <ShoppingBag className="h-8 w-8" />
                  </div>
                )}
              </Link>

              <div className="flex flex-1 flex-col justify-between">
                <div>
                  <Link
                    to={`/products/${item.productId}`}
                    className="font-medium text-gray-900 hover:text-primary-600"
                  >
                    {item.name}
                  </Link>
                  <p className="mt-1 text-sm font-bold text-primary-600">{formatPrice(item.price)}</p>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center rounded-lg border border-gray-300">
                    <button
                      onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                      className="px-2 py-1 hover:bg-gray-50"
                    >
                      <Minus className="h-3 w-3" />
                    </button>
                    <span className="w-8 text-center text-sm">{item.quantity}</span>
                    <button
                      onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                      className="px-2 py-1 hover:bg-gray-50"
                    >
                      <Plus className="h-3 w-3" />
                    </button>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-sm font-bold">{formatPrice(item.price * item.quantity)}</span>
                    <button
                      onClick={() => removeItem(item.productId)}
                      className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-500"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-6 lg:sticky lg:top-24 lg:self-start">
          <h3 className="text-lg font-bold text-gray-900">주문 요약</h3>
          <div className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">상품 수</span>
              <span>{items.length}종</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">총 수량</span>
              <span>{items.reduce((s, i) => s + i.quantity, 0)}개</span>
            </div>
            <div className="my-3 border-t" />
            <div className="flex justify-between text-base font-bold">
              <span>합계</span>
              <span className="text-primary-600">{formatPrice(totalPrice())}</span>
            </div>
          </div>
          <Button
            className="mt-6 w-full"
            onClick={() => {
              if (!isAuthenticated) {
                navigate('/login');
                return;
              }
              navigate('/checkout');
            }}
          >
            주문하기
          </Button>
        </div>
      </div>
    </div>
  );
}

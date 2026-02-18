import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ShoppingCart, Minus, Plus, ArrowLeft, Package, Loader2 } from 'lucide-react';
import { productApi } from '@/api/products';
import { useCartStore } from '@/stores/cartStore';
import { formatPrice } from '@/lib/utils';
import Button from '@/components/common/Button';

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);
  const addItem = useCartStore((s) => s.addItem);

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productApi.getById(Number(id)).then((r) => r.data.data!),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!product) {
    return <p className="py-20 text-center text-gray-500">상품을 찾을 수 없습니다.</p>;
  }

  const handleAddCart = () => {
    addItem(
      {
        productId: product.id,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl,
        stockQuantity: product.stockQuantity,
      },
      quantity
    );
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  const outOfStock = product.stockQuantity === 0;

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" /> 뒤로가기
      </button>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="aspect-square overflow-hidden rounded-xl bg-gray-100">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full items-center justify-center">
              <Package className="h-24 w-24 text-gray-300" />
            </div>
          )}
        </div>

        <div className="flex flex-col justify-between">
          <div>
            {product.categoryName && (
              <p className="text-sm text-gray-400">{product.categoryName}</p>
            )}
            <h1 className="mt-1 text-2xl font-bold text-gray-900">{product.name}</h1>
            <p className="mt-4 text-3xl font-bold text-primary-600">{formatPrice(product.price)}</p>

            {product.description && (
              <p className="mt-6 whitespace-pre-line text-sm leading-relaxed text-gray-600">
                {product.description}
              </p>
            )}

            <div className="mt-6">
              <span
                className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${
                  outOfStock ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                }`}
              >
                {outOfStock ? '품절' : `재고 ${product.stockQuantity}개`}
              </span>
            </div>
          </div>

          <div className="mt-8 space-y-4">
            <div className="flex items-center gap-3">
              <span className="text-sm font-medium text-gray-700">수량</span>
              <div className="flex items-center rounded-lg border border-gray-300">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="px-3 py-2 hover:bg-gray-50"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-12 text-center text-sm font-medium">{quantity}</span>
                <button
                  onClick={() => setQuantity(Math.min(product.stockQuantity, quantity + 1))}
                  className="px-3 py-2 hover:bg-gray-50"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
              <span className="text-sm text-gray-500">{formatPrice(product.price * quantity)}</span>
            </div>

            <div className="flex gap-3">
              <Button
                onClick={handleAddCart}
                variant="outline"
                className="flex-1"
                disabled={outOfStock}
              >
                <ShoppingCart className="mr-2 h-4 w-4" />
                {added ? '담았습니다!' : '장바구니'}
              </Button>
              <Button
                onClick={() => {
                  handleAddCart();
                  navigate('/cart');
                }}
                className="flex-1"
                disabled={outOfStock}
              >
                바로 구매
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

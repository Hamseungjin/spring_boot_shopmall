import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Edit, Trash2, Loader2, Package, Search } from 'lucide-react';
import { productApi } from '@/api/products';
import { formatPrice } from '@/lib/utils';
import Button from '@/components/common/Button';
import type { Product } from '@/types';

export default function AdminProductListPage() {
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['adminProducts', page, keyword],
    queryFn: () =>
      keyword
        ? productApi.search({ keyword, page, size: 20 }).then((r) => r.data.data!)
        : productApi.getAll(page, 20).then((r) => r.data.data!),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => productApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminProducts'] }),
  });

  const handleDelete = (product: Product) => {
    if (window.confirm(`"${product.name}" 상품을 삭제하시겠습니까?`)) {
      deleteMutation.mutate(product.id);
    }
  };

  const products = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">상품 관리</h1>
        <div className="flex gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="상품 검색..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPage(0);
              }}
              className="rounded-lg border border-gray-300 py-2 pl-10 pr-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <Link to="/admin/products/new">
            <Button>
              <Plus className="mr-1 h-4 w-4" /> 상품 등록
            </Button>
          </Link>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        </div>
      ) : products.length === 0 ? (
        <div className="flex flex-col items-center py-20 text-gray-400">
          <Package className="h-16 w-16" />
          <p className="mt-4 text-lg">등록된 상품이 없습니다</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-gray-200 bg-gray-50">
              <tr>
                <th className="px-4 py-3 font-medium text-gray-500">ID</th>
                <th className="px-4 py-3 font-medium text-gray-500">상품명</th>
                <th className="px-4 py-3 font-medium text-gray-500">카테고리</th>
                <th className="px-4 py-3 font-medium text-gray-500">가격</th>
                <th className="px-4 py-3 font-medium text-gray-500">재고</th>
                <th className="px-4 py-3 font-medium text-gray-500">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {products.map((product) => (
                <tr key={product.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">{product.id}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-gray-100">
                        {product.imageUrl ? (
                          <img
                            src={product.imageUrl}
                            alt={product.name}
                            className="h-full w-full object-cover"
                          />
                        ) : (
                          <div className="flex h-full items-center justify-center">
                            <Package className="h-5 w-5 text-gray-300" />
                          </div>
                        )}
                      </div>
                      <span className="font-medium text-gray-900">{product.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{product.categoryName ?? '-'}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {formatPrice(product.price)}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                        product.stockQuantity === 0
                          ? 'bg-red-100 text-red-700'
                          : product.stockQuantity < 10
                            ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-green-100 text-green-700'
                      }`}
                    >
                      {product.stockQuantity}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      <Link
                        to={`/admin/products/${product.id}/edit`}
                        className="rounded p-1.5 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                      >
                        <Edit className="h-4 w-4" />
                      </Link>
                      <button
                        onClick={() => handleDelete(product)}
                        className="rounded p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
                        disabled={deleteMutation.isPending}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
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

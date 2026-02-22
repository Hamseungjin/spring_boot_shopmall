import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Loader2, Package, SlidersHorizontal } from 'lucide-react';
import { productApi, type ProductSearchParams } from '@/api/products';
import { formatPrice } from '@/lib/utils';
import type { Product } from '@/types';
import Button from '@/components/common/Button';
import CategoryTreeSidebar from '@/components/products/CategoryTreeSidebar';

export default function ProductSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get('keyword') ?? '';
  const categoryId = searchParams.get('categoryId')
    ? Number(searchParams.get('categoryId'))
    : undefined;
  const minPrice = searchParams.get('minPrice')
    ? Number(searchParams.get('minPrice'))
    : undefined;
  const maxPrice = searchParams.get('maxPrice')
    ? Number(searchParams.get('maxPrice'))
    : undefined;
  const sortBy = searchParams.get('sortBy') ?? 'createdAt';
  const page = Number(searchParams.get('page') ?? '0');

  const [localKeyword, setLocalKeyword] = useState(keyword);
  const [localMinPrice, setLocalMinPrice] = useState(minPrice?.toString() ?? '');
  const [localMaxPrice, setLocalMaxPrice] = useState(maxPrice?.toString() ?? '');
  const [showFilter, setShowFilter] = useState(false);

  const params: ProductSearchParams = {
    keyword: keyword || undefined,
    categoryId,
    minPrice,
    maxPrice,
    sortBy,
    sortDirection: 'desc',
    page,
    size: 12,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['productSearch', params],
    queryFn: () => productApi.search(params).then((r) => r.data.data!),
  });

  const updateParams = (updates: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([k, v]) => {
      if (v === undefined || v === '') {
        next.delete(k);
      } else {
        next.set(k, v);
      }
    });
    next.delete('page');
    setSearchParams(next);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    updateParams({
      keyword: localKeyword || undefined,
      minPrice: localMinPrice || undefined,
      maxPrice: localMaxPrice || undefined,
    });
  };

  const goToPage = (p: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('page', String(p));
    setSearchParams(next);
  };

  const products: Product[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">상품 검색</h1>
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="relative flex-1 sm:w-80">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="검색어를 입력하세요..."
              value={localKeyword}
              onChange={(e) => setLocalKeyword(e.target.value)}
              className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <Button type="submit" size="md">
            검색
          </Button>
          <button
            type="button"
            onClick={() => setShowFilter(!showFilter)}
            className="rounded-lg border border-gray-300 p-2 hover:bg-gray-50"
          >
            <SlidersHorizontal className="h-5 w-5 text-gray-500" />
          </button>
        </form>
      </div>

      {showFilter && (
        <div className="flex flex-wrap items-end gap-3 rounded-lg border border-gray-200 bg-white p-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-gray-500">최소 가격</label>
            <input
              type="number"
              placeholder="0"
              value={localMinPrice}
              onChange={(e) => setLocalMinPrice(e.target.value)}
              className="w-32 rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-gray-500">최대 가격</label>
            <input
              type="number"
              placeholder="∞"
              value={localMaxPrice}
              onChange={(e) => setLocalMaxPrice(e.target.value)}
              className="w-32 rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <select
            value={sortBy}
            onChange={(e) => updateParams({ sortBy: e.target.value })}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="createdAt">최신순</option>
            <option value="price">가격순</option>
            <option value="name">이름순</option>
          </select>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              updateParams({
                minPrice: localMinPrice || undefined,
                maxPrice: localMaxPrice || undefined,
              });
            }}
          >
            필터 적용
          </Button>
        </div>
      )}

      {keyword && (
        <p className="text-sm text-gray-500">
          &ldquo;<span className="font-medium text-gray-900">{keyword}</span>&rdquo; 검색 결과
          {data && <span className="ml-1">({data.totalElements}건)</span>}
        </p>
      )}

      <div className="flex gap-6">
        <div className="hidden lg:block">
          <CategoryTreeSidebar
            selectedId={categoryId}
            onSelect={(id) => updateParams({ categoryId: id?.toString() })}
          />
        </div>

        <div className="min-w-0 flex-1">
          {isLoading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
            </div>
          ) : products.length === 0 ? (
            <div className="flex flex-col items-center py-20 text-gray-400">
              <Package className="h-16 w-16" />
              <p className="mt-4 text-lg">검색 결과가 없습니다</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {products.map((product) => (
                  <Link
                    key={product.id}
                    to={`/products/${product.id}`}
                    className="group overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm transition-shadow hover:shadow-md"
                  >
                    <div className="aspect-square bg-gray-100">
                      {product.imageUrl ? (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="h-full w-full object-cover transition-transform group-hover:scale-105"
                        />
                      ) : (
                        <div className="flex h-full items-center justify-center">
                          <Package className="h-12 w-12 text-gray-300" />
                        </div>
                      )}
                    </div>
                    <div className="p-3">
                      {product.categoryName && (
                        <p className="text-xs text-gray-400">{product.categoryName}</p>
                      )}
                      <h3 className="mt-1 line-clamp-2 text-sm font-medium text-gray-900">
                        {product.name}
                      </h3>
                      <p className="mt-1 text-base font-bold text-primary-600">
                        {formatPrice(product.price)}
                      </p>
                      {product.stockQuantity === 0 && (
                        <span className="mt-1 inline-block rounded bg-red-100 px-2 py-0.5 text-xs text-red-600">
                          품절
                        </span>
                      )}
                    </div>
                  </Link>
                ))}
              </div>

              {totalPages > 1 && (
                <div className="mt-6 flex items-center justify-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => goToPage(page - 1)}
                  >
                    이전
                  </Button>
                  <span className="text-sm text-gray-500">
                    {page + 1} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => goToPage(page + 1)}
                  >
                    다음
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

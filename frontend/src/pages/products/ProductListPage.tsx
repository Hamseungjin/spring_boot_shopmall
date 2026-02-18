import { useState, useRef, useCallback, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { Search, SlidersHorizontal, Loader2, Package } from 'lucide-react';
import { productApi, type ProductSearchParams } from '@/api/products';
import { formatPrice } from '@/lib/utils';
import type { Product, Category } from '@/types';
import Input from '@/components/common/Input';
import Button from '@/components/common/Button';

export default function ProductListPage() {
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [sortBy, setSortBy] = useState('createdAt');
  const [showFilter, setShowFilter] = useState(false);
  const observerRef = useRef<HTMLDivElement>(null);

  const { data: categoriesRes } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories().then((r) => r.data.data),
  });

  const buildParams = (page: number): ProductSearchParams => ({
    keyword: keyword || undefined,
    categoryId,
    sortBy,
    sortDirection: 'desc',
    page,
    size: 12,
    inStock: true,
  });

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useInfiniteQuery({
    queryKey: ['products', keyword, categoryId, sortBy],
    queryFn: ({ pageParam = 0 }) =>
      productApi.search(buildParams(pageParam)).then((r) => r.data.data!),
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
    initialPageParam: 0,
  });

  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [fetchNextPage, hasNextPage, isFetchingNextPage]
  );

  useEffect(() => {
    const el = observerRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(handleObserver, { threshold: 0.1 });
    observer.observe(el);
    return () => observer.disconnect();
  }, [handleObserver]);

  const products: Product[] = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">상품 목록</h1>
        <div className="flex gap-2">
          <div className="relative flex-1 sm:w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="상품 검색..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-3 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <button
            onClick={() => setShowFilter(!showFilter)}
            className="rounded-lg border border-gray-300 p-2 hover:bg-gray-50"
          >
            <SlidersHorizontal className="h-5 w-5 text-gray-500" />
          </button>
        </div>
      </div>

      {showFilter && (
        <div className="flex flex-wrap gap-3 rounded-lg border border-gray-200 bg-white p-4">
          <select
            value={categoryId ?? ''}
            onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="">전체 카테고리</option>
            {categoriesRes?.map((c: Category) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="createdAt">최신순</option>
            <option value="price">가격순</option>
            <option value="name">이름순</option>
          </select>
        </div>
      )}

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
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
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
      )}

      <div ref={observerRef} className="flex justify-center py-4">
        {isFetchingNextPage && <Loader2 className="h-6 w-6 animate-spin text-primary-500" />}
      </div>
    </div>
  );
}

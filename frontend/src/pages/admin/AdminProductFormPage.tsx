import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Upload, Loader2, Image as ImageIcon } from 'lucide-react';
import { productApi } from '@/api/products';
import Button from '@/components/common/Button';
import Input from '@/components/common/Input';
import type { Category } from '@/types';

const schema = z.object({
  name: z.string().min(1, '상품명을 입력해주세요.'),
  description: z.string().optional(),
  price: z.coerce.number().min(0, '가격은 0 이상이어야 합니다.'),
  stockQuantity: z.coerce.number().min(0, '재고는 0 이상이어야 합니다.'),
  categoryId: z.coerce.number().optional(),
});

type FormData = z.infer<typeof schema>;

export default function AdminProductFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEdit = !!id;

  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string>('');
  const [error, setError] = useState('');

  const { data: product, isLoading: loadingProduct } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productApi.getById(Number(id)).then((r) => r.data.data!),
    enabled: isEdit,
  });

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories().then((r) => r.data.data ?? []),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (product) {
      reset({
        name: product.name,
        description: product.description ?? '',
        price: product.price,
        stockQuantity: product.stockQuantity,
        categoryId: product.categoryId ?? undefined,
      });
      if (product.imageUrl) {
        setImagePreview(product.imageUrl);
      }
    }
  }, [product, reset]);

  const createMutation = useMutation({
    mutationFn: async (data: FormData) => {
      const res = await productApi.create({
        name: data.name,
        description: data.description,
        price: data.price,
        stockQuantity: data.stockQuantity,
        categoryId: data.categoryId,
      });
      const created = res.data.data!;
      if (imageFile) {
        await productApi.uploadImage(created.id, imageFile);
      }
      return created;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminProducts'] });
      navigate('/admin/products');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '상품 등록에 실패했습니다.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (data: FormData) => {
      const res = await productApi.update(Number(id), {
        name: data.name,
        description: data.description,
        price: data.price,
        stockQuantity: data.stockQuantity,
        categoryId: data.categoryId,
      });
      if (imageFile) {
        await productApi.uploadImage(Number(id), imageFile);
      }
      return res.data.data!;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminProducts'] });
      queryClient.invalidateQueries({ queryKey: ['product', id] });
      navigate('/admin/products');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || '상품 수정에 실패했습니다.');
    },
  });

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const onSubmit = (data: FormData) => {
    setError('');
    if (isEdit) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data);
    }
  };

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  if (isEdit && loadingProduct) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" /> 뒤로가기
      </button>

      <h1 className="text-2xl font-bold text-gray-900">
        {isEdit ? '상품 수정' : '상품 등록'}
      </h1>

      {error && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</div>}

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-5 lg:col-span-2">
          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="mb-4 text-lg font-bold text-gray-900">기본 정보</h3>
            <div className="space-y-4">
              <Input
                label="상품명"
                placeholder="상품명을 입력하세요"
                error={errors.name?.message}
                {...register('name')}
              />
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">상품 설명</label>
                <textarea
                  placeholder="상품 설명을 입력하세요"
                  rows={4}
                  className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  {...register('description')}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="가격"
                  type="number"
                  placeholder="0"
                  error={errors.price?.message}
                  {...register('price')}
                />
                <Input
                  label="재고 수량"
                  type="number"
                  placeholder="0"
                  error={errors.stockQuantity?.message}
                  {...register('stockQuantity')}
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">카테고리</label>
                <select
                  className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  {...register('categoryId')}
                >
                  <option value="">카테고리 선택</option>
                  {categories?.map((c: Category) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-5">
          <div className="rounded-xl border border-gray-200 bg-white p-6">
            <h3 className="mb-4 text-lg font-bold text-gray-900">상품 이미지</h3>
            <div className="space-y-4">
              {imagePreview ? (
                <div className="aspect-square overflow-hidden rounded-lg bg-gray-100">
                  <img
                    src={imagePreview}
                    alt="미리보기"
                    className="h-full w-full object-cover"
                  />
                </div>
              ) : (
                <div className="flex aspect-square items-center justify-center rounded-lg border-2 border-dashed border-gray-300 bg-gray-50">
                  <ImageIcon className="h-12 w-12 text-gray-300" />
                </div>
              )}
              <label className="flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50">
                <Upload className="h-4 w-4" />
                이미지 선택
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="hidden"
                />
              </label>
            </div>
          </div>

          <Button type="submit" className="w-full" size="lg" loading={isSubmitting}>
            {isEdit ? '수정 완료' : '상품 등록'}
          </Button>
        </div>
      </form>
    </div>
  );
}

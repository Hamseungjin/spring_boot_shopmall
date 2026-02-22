import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Loader2,
  FolderTree,
  Plus,
  ChevronRight,
  ChevronDown,
  Folder,
  FolderOpen,
} from 'lucide-react';
import { productApi } from '@/api/products';
import Button from '@/components/common/Button';
import Input from '@/components/common/Input';
import type { Category } from '@/types';

const schema = z.object({
  name: z.string().min(1, '카테고리명을 입력해주세요.'),
  description: z.string().optional(),
  parentId: z.coerce.number().optional(),
  sortOrder: z.coerce.number().optional(),
});

type FormData = z.infer<typeof schema>;

export default function AdminCategoryPage() {
  const [showForm, setShowForm] = useState(false);
  const queryClient = useQueryClient();

  const { data: tree, isLoading: loadingTree } = useQuery({
    queryKey: ['categoryTree'],
    queryFn: () => productApi.getCategoryTree().then((r) => r.data.data ?? []),
  });

  const { data: flatCategories } = useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories().then((r) => r.data.data ?? []),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const createMutation = useMutation({
    mutationFn: (data: FormData) =>
      productApi.createCategory({
        name: data.name,
        description: data.description,
        parentId: data.parentId || undefined,
        sortOrder: data.sortOrder,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categoryTree'] });
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      reset();
      setShowForm(false);
    },
  });

  const onSubmit = (data: FormData) => createMutation.mutate(data);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">카테고리 관리</h1>
        <Button onClick={() => setShowForm(!showForm)}>
          <Plus className="mr-1 h-4 w-4" /> 카테고리 추가
        </Button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="rounded-xl border border-gray-200 bg-white p-6"
        >
          <h3 className="mb-4 text-lg font-bold text-gray-900">새 카테고리</h3>
          {createMutation.isError && (
            <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
              카테고리 등록에 실패했습니다.
            </div>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label="카테고리명"
              placeholder="카테고리명"
              error={errors.name?.message}
              {...register('name')}
            />
            <Input
              label="설명 (선택)"
              placeholder="카테고리 설명"
              {...register('description')}
            />
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">상위 카테고리</label>
              <select
                className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
                {...register('parentId')}
              >
                <option value="">없음 (최상위)</option>
                {flatCategories?.map((c: Category) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <Input
              label="정렬 순서"
              type="number"
              placeholder="0"
              {...register('sortOrder')}
            />
          </div>
          <div className="mt-4 flex gap-2">
            <Button type="submit" loading={createMutation.isPending}>
              등록
            </Button>
            <Button type="button" variant="ghost" onClick={() => setShowForm(false)}>
              취소
            </Button>
          </div>
        </form>
      )}

      {loadingTree ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        </div>
      ) : !tree || tree.length === 0 ? (
        <div className="flex flex-col items-center py-20 text-gray-400">
          <FolderTree className="h-16 w-16" />
          <p className="mt-4 text-lg">등록된 카테고리가 없습니다</p>
        </div>
      ) : (
        <div className="rounded-xl border border-gray-200 bg-white p-6">
          <h3 className="mb-4 text-lg font-bold text-gray-900">카테고리 트리</h3>
          <ul className="space-y-1">
            {tree.map((cat) => (
              <TreeNode key={cat.id} category={cat} depth={0} />
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function TreeNode({ category, depth }: { category: Category; depth: number }) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = category.children && category.children.length > 0;

  return (
    <li>
      <div
        className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50"
        style={{ paddingLeft: `${depth * 20 + 8}px` }}
      >
        {hasChildren ? (
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-gray-400 hover:text-gray-600"
          >
            {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </button>
        ) : (
          <span className="w-4" />
        )}
        {hasChildren ? (
          expanded ? (
            <FolderOpen className="h-4 w-4 text-primary-500" />
          ) : (
            <Folder className="h-4 w-4 text-primary-500" />
          )
        ) : (
          <Folder className="h-4 w-4 text-gray-400" />
        )}
        <span className="text-sm font-medium text-gray-700">{category.name}</span>
        {category.description && (
          <span className="text-xs text-gray-400">— {category.description}</span>
        )}
        <span className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">
          ID: {category.id}
        </span>
      </div>
      {hasChildren && expanded && (
        <ul className="space-y-0.5">
          {category.children!.map((child) => (
            <TreeNode key={child.id} category={child} depth={depth + 1} />
          ))}
        </ul>
      )}
    </li>
  );
}

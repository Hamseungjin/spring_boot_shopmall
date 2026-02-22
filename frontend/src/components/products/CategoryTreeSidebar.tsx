import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronRight, ChevronDown, FolderOpen, Folder } from 'lucide-react';
import { productApi } from '@/api/products';
import type { Category } from '@/types';

interface CategoryTreeSidebarProps {
  selectedId?: number;
  onSelect: (id: number | undefined) => void;
}

export default function CategoryTreeSidebar({ selectedId, onSelect }: CategoryTreeSidebarProps) {
  const { data: categories } = useQuery({
    queryKey: ['categoryTree'],
    queryFn: () => productApi.getCategoryTree().then((r) => r.data.data ?? []),
  });

  return (
    <aside className="w-full shrink-0 lg:w-56">
      <div className="rounded-xl border border-gray-200 bg-white p-4">
        <h3 className="text-sm font-bold text-gray-900">카테고리</h3>
        <ul className="mt-3 space-y-0.5">
          <li>
            <button
              onClick={() => onSelect(undefined)}
              className={`w-full rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                selectedId === undefined
                  ? 'bg-primary-50 font-medium text-primary-700'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              전체
            </button>
          </li>
          {categories?.map((cat) => (
            <CategoryNode
              key={cat.id}
              category={cat}
              selectedId={selectedId}
              onSelect={onSelect}
              depth={0}
            />
          ))}
        </ul>
      </div>
    </aside>
  );
}

function CategoryNode({
  category,
  selectedId,
  onSelect,
  depth,
}: {
  category: Category;
  selectedId?: number;
  onSelect: (id: number | undefined) => void;
  depth: number;
}) {
  const [expanded, setExpanded] = useState(false);
  const hasChildren = category.children && category.children.length > 0;
  const isSelected = selectedId === category.id;

  return (
    <li>
      <div className="flex items-center">
        {hasChildren && (
          <button
            onClick={() => setExpanded(!expanded)}
            className="shrink-0 rounded p-0.5 text-gray-400 hover:text-gray-600"
          >
            {expanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
          </button>
        )}
        <button
          onClick={() => onSelect(category.id)}
          className={`flex flex-1 items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm transition-colors ${
            isSelected
              ? 'bg-primary-50 font-medium text-primary-700'
              : 'text-gray-600 hover:bg-gray-50'
          }`}
          style={{ paddingLeft: hasChildren ? undefined : `${(depth + 1) * 8 + 8}px` }}
        >
          {hasChildren ? (
            expanded ? <FolderOpen className="h-3.5 w-3.5 shrink-0 text-gray-400" /> : <Folder className="h-3.5 w-3.5 shrink-0 text-gray-400" />
          ) : null}
          <span className="truncate">{category.name}</span>
        </button>
      </div>
      {hasChildren && expanded && (
        <ul className="ml-3 space-y-0.5">
          {category.children!.map((child) => (
            <CategoryNode
              key={child.id}
              category={child}
              selectedId={selectedId}
              onSelect={onSelect}
              depth={depth + 1}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

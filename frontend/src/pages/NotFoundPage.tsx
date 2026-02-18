import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';
import Button from '@/components/common/Button';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <p className="text-6xl font-bold text-primary-600">404</p>
      <h1 className="mt-4 text-2xl font-bold text-gray-900">페이지를 찾을 수 없습니다</h1>
      <p className="mt-2 text-gray-500">요청하신 페이지가 존재하지 않거나 이동되었습니다.</p>
      <Link to="/" className="mt-6">
        <Button>
          <Home className="mr-2 h-4 w-4" />
          홈으로 돌아가기
        </Button>
      </Link>
    </div>
  );
}

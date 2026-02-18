import { createBrowserRouter } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import HomePage from '@/pages/HomePage';
import NotFoundPage from '@/pages/NotFoundPage';
import LoginPage from '@/pages/auth/LoginPage';
import SignupPage from '@/pages/auth/SignupPage';

export const router = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'signup', element: <SignupPage /> },
      { path: 'products', element: <PlaceholderPage title="상품 목록" /> },
      { path: 'products/:id', element: <PlaceholderPage title="상품 상세" /> },
      {
        path: 'orders',
        element: (
          <ProtectedRoute>
            <PlaceholderPage title="주문 내역" />
          </ProtectedRoute>
        ),
      },
      {
        path: 'mypage',
        element: (
          <ProtectedRoute>
            <PlaceholderPage title="마이페이지" />
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin',
        element: (
          <ProtectedRoute requireAdmin>
            <PlaceholderPage title="관리자 대시보드" />
          </ProtectedRoute>
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
        <p className="mt-2 text-gray-500">이 페이지는 다음 단계에서 구현됩니다.</p>
      </div>
    </div>
  );
}

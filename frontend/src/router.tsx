import { createBrowserRouter } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import HomePage from '@/pages/HomePage';
import NotFoundPage from '@/pages/NotFoundPage';
import LoginPage from '@/pages/auth/LoginPage';
import SignupPage from '@/pages/auth/SignupPage';
import ProductListPage from '@/pages/products/ProductListPage';
import ProductDetailPage from '@/pages/products/ProductDetailPage';
import ProductSearchPage from '@/pages/products/ProductSearchPage';
import CartPage from '@/pages/CartPage';
import CheckoutPage from '@/pages/orders/CheckoutPage';
import OrderListPage from '@/pages/orders/OrderListPage';
import OrderDetailPage from '@/pages/orders/OrderDetailPage';
import MyPage from '@/pages/MyPage';
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage';
import AdminProductListPage from '@/pages/admin/AdminProductListPage';
import AdminProductFormPage from '@/pages/admin/AdminProductFormPage';
import AdminOrderListPage from '@/pages/admin/AdminOrderListPage';
import AdminCategoryPage from '@/pages/admin/AdminCategoryPage';



export const router = createBrowserRouter(
  [
    {
      element: <Layout />,
      children: [
        { index: true, element: <HomePage /> },
        { path: 'login', element: <LoginPage /> },
        { path: 'signup', element: <SignupPage /> },
        { path: 'products', element: <ProductListPage /> },
        { path: 'products/search', element: <ProductSearchPage /> },
        { path: 'products/:id', element: <ProductDetailPage /> },
        { path: 'cart', element: <CartPage /> },
        {
          path: 'checkout',
          element: <ProtectedRoute><CheckoutPage /></ProtectedRoute>,
        },
        {
          path: 'orders',
          element: <ProtectedRoute><OrderListPage /></ProtectedRoute>,
        },
        {
          path: 'orders/:id',
          element: <ProtectedRoute><OrderDetailPage /></ProtectedRoute>,
        },
        {
          path: 'mypage',
          element: <ProtectedRoute><MyPage /></ProtectedRoute>,
        },
        {
          path: 'admin',
          element: <ProtectedRoute requireAdmin><AdminDashboardPage /></ProtectedRoute>,
        },
        {
          path: 'admin/products',
          element: <ProtectedRoute requireAdmin><AdminProductListPage /></ProtectedRoute>,
        },
        {
          path: 'admin/products/new',
          element: <ProtectedRoute requireAdmin><AdminProductFormPage /></ProtectedRoute>,
        },
        {
          path: 'admin/products/:id/edit',
          element: <ProtectedRoute requireAdmin><AdminProductFormPage /></ProtectedRoute>,
        },
        {
          path: 'admin/orders',
          element: <ProtectedRoute requireAdmin><AdminOrderListPage /></ProtectedRoute>,
        },
        {
          path: 'admin/categories',
          element: <ProtectedRoute requireAdmin><AdminCategoryPage /></ProtectedRoute>,
        },
        { path: '*', element: <NotFoundPage /> },
      ],
    },
  ],
  { basename: '/app' }
);
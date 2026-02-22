import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Menu,
  X,
  ShoppingCart,
  User,
  LogOut,
  LayoutDashboard,
  Package,
  Search,
} from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { useCartStore } from '@/stores/cartStore';
import api from '@/lib/axios';

export default function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { isAuthenticated, member, logout: clearAuth } = useAuthStore();
  const cartItemCount = useCartStore((s) => s.items.length);
  const navigate = useNavigate();

  const isAdmin = member?.role === 'ADMIN';

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // 로그아웃 API 실패해도 로컬 상태는 정리
    } finally {
      clearAuth();
      navigate('/login');
    }
  };

  const navLinks = [
    { to: '/products', label: '상품', icon: Package },
    { to: '/orders', label: '주문내역', icon: ShoppingCart, auth: true },
  ];

  return (
    <header className="sticky top-0 z-50 border-b border-gray-200 bg-white/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2">
          <Package className="h-7 w-7 text-primary-600" />
          <span className="text-lg font-bold text-gray-900">ShopMall</span>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden items-center gap-1 md:flex">
          {navLinks.map(
            (link) =>
              (!link.auth || isAuthenticated) && (
                <Link
                  key={link.to}
                  to={link.to}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-900"
                >
                  <link.icon className="h-4 w-4" />
                  {link.label}
                </Link>
              )
          )}

          {isAdmin && (
            <Link
              to="/admin"
              className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-primary-600 transition-colors hover:bg-primary-50"
            >
              <LayoutDashboard className="h-4 w-4" />
              관리자
            </Link>
          )}
        </nav>

        {/* Desktop Right */}
        <div className="hidden items-center gap-2 md:flex">
          <Link
            to="/products/search"
            className="rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
          >
            <Search className="h-5 w-5" />
          </Link>
          <Link
            to="/cart"
            className="relative rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
          >
            <ShoppingCart className="h-5 w-5" />
            {cartItemCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary-600 text-[10px] font-bold text-white">
                {cartItemCount}
              </span>
            )}
          </Link>

          {isAuthenticated ? (
            <div className="flex items-center gap-2">
              <Link
                to="/mypage"
                className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
              >
                <User className="h-4 w-4" />
                {member?.name}
              </Link>
              <button
                onClick={handleLogout}
                className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 transition-colors hover:bg-gray-100 hover:text-red-600"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="rounded-lg px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-100"
              >
                로그인
              </Link>
              <Link
                to="/signup"
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-700"
              >
                회원가입
              </Link>
            </div>
          )}
        </div>

        {/* Mobile Hamburger */}
        <button
          className="rounded-lg p-2 text-gray-600 md:hidden"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label="메뉴 열기"
        >
          {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </div>

      {/* Mobile Menu */}
      {mobileOpen && (
        <div className="border-t border-gray-200 bg-white md:hidden">
          <nav className="space-y-1 px-4 py-3">
            {navLinks.map(
              (link) =>
                (!link.auth || isAuthenticated) && (
                  <Link
                    key={link.to}
                    to={link.to}
                    onClick={() => setMobileOpen(false)}
                    className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
                  >
                    <link.icon className="h-5 w-5 text-gray-400" />
                    {link.label}
                  </Link>
                )
            )}

            {isAdmin && (
              <Link
                to="/admin"
                onClick={() => setMobileOpen(false)}
                className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-primary-600 hover:bg-primary-50"
              >
                <LayoutDashboard className="h-5 w-5" />
                관리자 대시보드
              </Link>
            )}

            <div className="my-2 border-t border-gray-200" />

            {isAuthenticated ? (
              <>
                <Link
                  to="/mypage"
                  onClick={() => setMobileOpen(false)}
                  className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
                >
                  <User className="h-5 w-5 text-gray-400" />
                  {member?.name} 님
                </Link>
                <button
                  onClick={() => {
                    setMobileOpen(false);
                    handleLogout();
                  }}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50"
                >
                  <LogOut className="h-5 w-5" />
                  로그아웃
                </button>
              </>
            ) : (
              <div className="flex gap-2 pt-1">
                <Link
                  to="/login"
                  onClick={() => setMobileOpen(false)}
                  className="flex-1 rounded-lg border border-gray-300 py-2.5 text-center text-sm font-medium text-gray-700"
                >
                  로그인
                </Link>
                <Link
                  to="/signup"
                  onClick={() => setMobileOpen(false)}
                  className="flex-1 rounded-lg bg-primary-600 py-2.5 text-center text-sm font-medium text-white"
                >
                  회원가입
                </Link>
              </div>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}

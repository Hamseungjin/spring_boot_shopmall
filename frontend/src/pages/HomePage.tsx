import { Link } from 'react-router-dom';
import { ArrowRight, Package, ShieldCheck, Truck } from 'lucide-react';
import Button from '@/components/common/Button';

export default function HomePage() {
  return (
    <div className="space-y-16 py-8">
      {/* Hero */}
      <section className="text-center">
        <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl">
          쇼핑의 새로운 기준,{' '}
          <span className="text-primary-600">ShopMall</span>
        </h1>
        <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-500">
          합리적인 가격과 빠른 배송으로 최고의 쇼핑 경험을 제공합니다.
        </p>
        <div className="mt-8 flex justify-center gap-4">
          <Link to="/products">
            <Button size="lg">
              상품 둘러보기
              <ArrowRight className="ml-2 h-5 w-5" />
            </Button>
          </Link>
          <Link to="/signup">
            <Button variant="outline" size="lg">
              회원가입
            </Button>
          </Link>
        </div>
      </section>

      {/* Features */}
      <section className="grid gap-8 sm:grid-cols-3">
        {[
          {
            icon: Package,
            title: '엄선된 상품',
            desc: '카테고리별 다양한 상품을 한눈에 비교하세요.',
          },
          {
            icon: Truck,
            title: '빠른 배송',
            desc: '주문부터 배송까지 실시간으로 확인할 수 있습니다.',
          },
          {
            icon: ShieldCheck,
            title: '안전한 결제',
            desc: '멱등성 보장 결제 시스템으로 안심하고 결제하세요.',
          },
        ].map((feature) => (
          <div
            key={feature.title}
            className="rounded-xl border border-gray-200 bg-white p-6 text-center shadow-sm"
          >
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-primary-50">
              <feature.icon className="h-6 w-6 text-primary-600" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">{feature.title}</h3>
            <p className="mt-2 text-sm text-gray-500">{feature.desc}</p>
          </div>
        ))}
      </section>
    </div>
  );
}

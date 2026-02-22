import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  User,
  Mail,
  Phone,
  MapPin,
  Calendar,
  ShieldCheck,
  ShoppingBag,
  Loader2,
} from 'lucide-react';
import { memberApi } from '@/api/members';
import { useAuthStore } from '@/stores/authStore';
import { formatDate } from '@/lib/utils';
import Button from '@/components/common/Button';

export default function MyPage() {
  const setMember = useAuthStore((s) => s.setMember);

  const { data: member, isLoading } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const res = await memberApi.getMe();
      const m = res.data.data!;
      setMember(m);
      return m;
    },
  });

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!member) {
    return <p className="py-20 text-center text-gray-500">회원 정보를 불러올 수 없습니다.</p>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">마이페이지</h1>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-6">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary-50">
              <User className="h-8 w-8 text-primary-600" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">{member.name}</h2>
              <span
                className={`mt-1 inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
                  member.role === 'ADMIN'
                    ? 'bg-purple-100 text-purple-700'
                    : 'bg-blue-100 text-blue-700'
                }`}
              >
                {member.role === 'ADMIN' ? '관리자' : '일반 회원'}
              </span>
            </div>
          </div>

          <div className="mt-6 space-y-4">
            <InfoRow icon={Mail} label="이메일" value={member.email} />
            <InfoRow icon={Phone} label="전화번호" value={member.phone ?? '미등록'} />
            <InfoRow icon={MapPin} label="주소" value={member.address ?? '미등록'} />
            <InfoRow icon={Calendar} label="가입일" value={formatDate(member.createdAt)} />
            <InfoRow
              icon={ShieldCheck}
              label="권한"
              value={member.role === 'ADMIN' ? '관리자' : '일반 회원'}
            />
          </div>
        </div>

        <div className="space-y-4">
          <Link
            to="/orders"
            className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white p-6 transition-shadow hover:shadow-md"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-orange-50">
              <ShoppingBag className="h-6 w-6 text-orange-600" />
            </div>
            <div>
              <h3 className="font-bold text-gray-900">주문 내역</h3>
              <p className="text-sm text-gray-500">주문 현황을 확인하세요</p>
            </div>
          </Link>

          {member.role === 'ADMIN' && (
            <Link
              to="/admin"
              className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white p-6 transition-shadow hover:shadow-md"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-purple-50">
                <ShieldCheck className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900">관리자 대시보드</h3>
                <p className="text-sm text-gray-500">관리자 페이지로 이동</p>
              </div>
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

function InfoRow({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Mail;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <Icon className="h-4 w-4 shrink-0 text-gray-400" />
      <span className="w-16 shrink-0 text-sm text-gray-500">{label}</span>
      <span className="text-sm text-gray-900">{value}</span>
    </div>
  );
}

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Package } from 'lucide-react';
import Button from '@/components/common/Button';
import Input from '@/components/common/Input';
import api from '@/lib/axios';
import type { ApiResponse, Member } from '@/types';

const schema = z
  .object({
    email: z.string().email('올바른 이메일을 입력해주세요.'),
    password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다.'),
    passwordConfirm: z.string(),
    name: z.string().min(1, '이름을 입력해주세요.'),
    phone: z.string().optional(),
    address: z.string().optional(),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['passwordConfirm'],
  });

type FormData = z.infer<typeof schema>;

export default function SignupPage() {
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setError('');
    try {
      const { passwordConfirm, ...payload } = data;
      const res = await api.post<ApiResponse<Member>>('/auth/signup', payload);
      if (res.data.success) {
        navigate('/login', { state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' } });
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.');
    }
  };

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <Package className="mx-auto h-10 w-10 text-primary-600" />
          <h1 className="mt-4 text-2xl font-bold text-gray-900">회원가입</h1>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {error && (
            <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</div>
          )}

          <Input
            label="이메일"
            type="email"
            placeholder="email@example.com"
            error={errors.email?.message}
            {...register('email')}
          />
          <Input
            label="비밀번호"
            type="password"
            placeholder="8자 이상"
            error={errors.password?.message}
            {...register('password')}
          />
          <Input
            label="비밀번호 확인"
            type="password"
            placeholder="비밀번호 재입력"
            error={errors.passwordConfirm?.message}
            {...register('passwordConfirm')}
          />
          <Input
            label="이름"
            placeholder="홍길동"
            error={errors.name?.message}
            {...register('name')}
          />
          <Input label="전화번호" placeholder="010-1234-5678" {...register('phone')} />
          <Input label="주소" placeholder="서울시 강남구..." {...register('address')} />

          <Button type="submit" loading={isSubmitting} className="w-full">
            가입하기
          </Button>
        </form>

        <p className="text-center text-sm text-gray-500">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="font-medium text-primary-600 hover:text-primary-700">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}

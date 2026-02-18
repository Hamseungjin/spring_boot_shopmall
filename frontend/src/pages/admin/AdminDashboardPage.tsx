import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';
import {
  DollarSign, ShoppingCart, Users, TrendingUp, Eye, Download, Loader2,
} from 'lucide-react';
import { adminApi } from '@/api/admin';
import { formatPrice, downloadCsv } from '@/lib/utils';
import Button from '@/components/common/Button';
import type { DashboardData } from '@/types';

function defaultRange() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  };
}

export default function AdminDashboardPage() {
  const [range, setRange] = useState(defaultRange);
  const [exporting, setExporting] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard', range.from, range.to],
    queryFn: () =>
      adminApi.getDashboard(range.from, range.to).then((r) => r.data.data!),
  });

  const handleExport = async () => {
    setExporting(true);
    try {
      const res = await adminApi.exportEventLogs(range.from, range.to);
      downloadCsv(res.data, `event_logs_${range.from}_${range.to}.csv`);
    } catch {
      alert('내보내기에 실패했습니다.');
    } finally {
      setExporting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  const dashboard: DashboardData | undefined = data;
  const kpi = dashboard?.kpi;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">관리자 대시보드</h1>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={range.from}
            onChange={(e) => setRange((r) => ({ ...r, from: e.target.value }))}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <span className="text-gray-400">~</span>
          <input
            type="date"
            value={range.to}
            onChange={(e) => setRange((r) => ({ ...r, to: e.target.value }))}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <Button variant="outline" size="sm" onClick={handleExport} loading={exporting}>
            <Download className="mr-1 h-4 w-4" /> CSV
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      {kpi && (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
          <KpiCard icon={DollarSign} label="누적 매출" value={formatPrice(kpi.totalRevenue)} color="text-green-600 bg-green-50" />
          <KpiCard icon={ShoppingCart} label="주문 건수" value={`${kpi.totalOrders}건`} color="text-blue-600 bg-blue-50" />
          <KpiCard icon={TrendingUp} label="결제 전환율" value={`${kpi.conversionRate.toFixed(1)}%`} color="text-purple-600 bg-purple-50" />
          <KpiCard icon={Users} label="신규 가입" value={`${kpi.newMembers}명`} color="text-orange-600 bg-orange-50" />
          <KpiCard icon={Eye} label="접속자" value={`${kpi.onlineUsers}명`} color="text-pink-600 bg-pink-50" />
        </div>
      )}

      {/* Daily Sales Chart */}
      {dashboard?.dailySales && dashboard.dailySales.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white p-6">
          <h3 className="mb-4 text-lg font-bold text-gray-900">일별 매출 추이</h3>
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={dashboard.dailySales}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `${(v / 10000).toFixed(0)}만`} />
              <Tooltip
                formatter={(value: number | undefined) => [formatPrice(value ?? 0), '매출']}
                labelFormatter={(l) => `${l}`}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="revenue"
                name="매출"
                stroke="#2563eb"
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
              <Line
                type="monotone"
                dataKey="orderCount"
                name="주문수"
                stroke="#16a34a"
                strokeWidth={2}
                dot={{ r: 3 }}
                yAxisId={0}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Category Sales Chart */}
      {dashboard?.categorySales && dashboard.categorySales.length > 0 && (
        <div className="rounded-xl border border-gray-200 bg-white p-6">
          <h3 className="mb-4 text-lg font-bold text-gray-900">카테고리별 매출</h3>
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={dashboard.categorySales}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="categoryName" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `${(v / 10000).toFixed(0)}만`} />
              <Tooltip formatter={(value: number | undefined) => [formatPrice(value ?? 0), '매출']} />
              <Legend />
              <Bar dataKey="revenue" name="매출" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              <Bar dataKey="quantity" name="판매수량" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

function KpiCard({
  icon: Icon,
  label,
  value,
  color,
}: {
  icon: typeof DollarSign;
  label: string;
  value: string;
  color: string;
}) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className={`inline-flex rounded-lg p-2 ${color}`}>
        <Icon className="h-5 w-5" />
      </div>
      <p className="mt-3 text-2xl font-bold text-gray-900">{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  );
}

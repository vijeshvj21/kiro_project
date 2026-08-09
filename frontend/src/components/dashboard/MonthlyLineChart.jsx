/**
 * MonthlyLineChart — line chart of 12 monthly expense totals for a given year.
 *
 * Props:
 *   monthlyData {Array<{ month: number, total: number }>}
 *     Array of 12 objects sorted by month 1–12.
 *
 * Requirements: 8.2
 */
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
} from 'recharts';

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function MonthlyLineChart({ monthlyData }) {
  // Map month number → abbreviated name for display
  const data = (monthlyData ?? []).map(({ month, total }) => ({
    name: MONTH_LABELS[(month - 1) % 12],
    total: Number(total) || 0,
  }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
        <XAxis
          dataKey="name"
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={56}
          tickFormatter={(v) => `₹${v}`}
        />
        <Tooltip
          formatter={(value) => [`₹${Number(value).toFixed(2)}`, 'Amount']}
        />
        <Line
          type="monotone"
          dataKey="total"
          stroke="#3b82f6"
          strokeWidth={2}
          dot={{ r: 3, fill: '#3b82f6' }}
          activeDot={{ r: 5 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

export default MonthlyLineChart;

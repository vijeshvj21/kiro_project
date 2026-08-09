/**
 * SideBySideBarChart — grouped bar chart comparing 12 months across two years.
 *
 * Props:
 *   data          {Array}  12 entries: { month, specifiedYearTotal, precedingYearTotal }
 *   specifiedYear {number} The selected year (rendered in blue)
 *   precedingYear {number} The preceding year (rendered in gray)
 *
 * Requirements: 10.1–10.6
 */
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  ResponsiveContainer,
} from 'recharts';

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function SideBySideBarChart({ data, specifiedYear, precedingYear }) {
  // Map month number (1–12) to abbreviated name and ensure 12 entries
  const chartData = (data ?? []).map((entry) => ({
    ...entry,
    monthLabel: MONTH_LABELS[(entry.month ?? 1) - 1] ?? String(entry.month),
  }));

  return (
    <ResponsiveContainer width="100%" height={250}>
      <BarChart
        data={chartData}
        margin={{ top: 4, right: 8, left: 0, bottom: 0 }}
        barCategoryGap="20%"
        barGap={2}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
        <XAxis
          dataKey="monthLabel"
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={52}
          tickFormatter={(v) => `₹${v}`}
        />
        <Tooltip
          formatter={(value, name) => [`₹${Number(value).toFixed(2)}`, name]}
          labelFormatter={(label) => label}
        />
        <Legend />
        <Bar
          dataKey="specifiedYearTotal"
          name={String(specifiedYear)}
          fill="#3b82f6"
          radius={[2, 2, 0, 0]}
        />
        <Bar
          dataKey="precedingYearTotal"
          name={String(precedingYear)}
          fill="#9ca3af"
          radius={[2, 2, 0, 0]}
        />
      </BarChart>
    </ResponsiveContainer>
  );
}

export default SideBySideBarChart;

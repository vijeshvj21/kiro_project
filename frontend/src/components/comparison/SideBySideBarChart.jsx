/**
 * SideBySideBarChart — grouped bar chart comparing 12 months across two years.
 * Clicking a month group opens a callback with the month index.
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

function SideBySideBarChart({ data, specifiedYear, precedingYear, onBarClick }) {
  const chartData = (data ?? []).map((entry) => ({
    ...entry,
    monthLabel: MONTH_LABELS[(entry.month ?? 1) - 1] ?? String(entry.month),
  }));

  const handleClick = (barData) => {
    if (onBarClick && barData && barData.month) {
      onBarClick(barData.month);
    }
  };

  return (
    <div>
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
            onClick={handleClick}
            cursor="pointer"
          />
          <Bar
            dataKey="precedingYearTotal"
            name={String(precedingYear)}
            fill="#9ca3af"
            radius={[2, 2, 0, 0]}
            onClick={handleClick}
            cursor="pointer"
          />
        </BarChart>
      </ResponsiveContainer>
      {onBarClick && (
        <p className="text-[10px] text-gray-400 text-center mt-1">Click a month to see category breakdown</p>
      )}
    </div>
  );
}

export default SideBySideBarChart;

import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

// Fixed colors per category for consistent visual identity
const CATEGORY_COLORS = {
  'Food': '#ef4444',        // Red
  'Transport': '#3b82f6',   // Blue
  'Shopping': '#f59e0b',    // Amber
  'Entertainment': '#8b5cf6', // Purple
  'Healthcare': '#10b981',  // Green
  'Utilities': '#06b6d4',   // Cyan
  'Education': '#ec4899',   // Pink
  'Investment': '#f97316',  // Orange
  'Other': '#6b7280',       // Gray
};

// Fallback colors for unknown categories
const FALLBACK_COLORS = ['#06b6d4', '#ec4899', '#84cc16', '#f97316', '#14b8a6', '#a855f7', '#eab308', '#0ea5e9'];

function getCategoryColor(categoryName, index) {
  return CATEGORY_COLORS[categoryName] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

/**
 * CategoryPieChart — renders a donut-style pie chart showing expense breakdown
 * by category. Each category has a consistent, fixed color for easy recognition.
 *
 * Props:
 *   data — array of { categoryName: string, total: number }
 */
export default function CategoryPieChart({ data }) {
  if (!data || data.length === 0) {
    return <p className="py-8 text-center text-sm text-gray-500">No category data available.</p>;
  }

  const chartData = data.map((item, index) => ({
    name: item.categoryName,
    value: Number(item.total),
    color: getCategoryColor(item.categoryName, index),
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={55}
          outerRadius={110}
          dataKey="value"
          label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
          labelLine={true}
        >
          {chartData.map((entry) => (
            <Cell key={entry.name} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip
          formatter={(value) => [`₹${Number(value).toFixed(2)}`, 'Amount']}
          contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb' }}
        />
        <Legend
          wrapperStyle={{ paddingTop: '16px' }}
          formatter={(value) => <span className="text-sm text-gray-700">{value}</span>}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}

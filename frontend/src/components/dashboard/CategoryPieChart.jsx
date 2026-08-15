import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

// Fixed colors per category for consistent visual identity
const CATEGORY_COLORS = {
  'Food': '#ef4444',
  'Transport': '#3b82f6',
  'Shopping': '#f59e0b',
  'Entertainment': '#8b5cf6',
  'Healthcare': '#10b981',
  'Utilities': '#06b6d4',
  'Education': '#ec4899',
  'Investment': '#f97316',
  'Savings': '#84cc16',
  'Other': '#6b7280',
};

const FALLBACK_COLORS = ['#06b6d4', '#ec4899', '#84cc16', '#f97316', '#14b8a6', '#a855f7', '#eab308', '#0ea5e9'];

function getCategoryColor(categoryName, index) {
  return CATEGORY_COLORS[categoryName] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

// Custom label renderer that positions text outside the pie with category color
const renderCustomLabel = ({ cx, cy, midAngle, outerRadius, name, percent, index, payload }) => {
  const RADIAN = Math.PI / 180;
  const radius = outerRadius + 20;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  const color = getCategoryColor(name, index);

  return (
    <text
      x={x}
      y={y}
      fill={color}
      textAnchor={x > cx ? 'start' : 'end'}
      dominantBaseline="central"
      fontSize={11}
      fontWeight={600}
    >
      {`${name} ${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

/**
 * CategoryPieChart — renders a donut-style pie chart showing expense breakdown
 * by category with labels positioned outside the chart.
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
          cy="45%"
          innerRadius={45}
          outerRadius={80}
          dataKey="value"
          label={renderCustomLabel}
          labelLine={true}
        >
          {chartData.map((entry) => (
            <Cell key={entry.name} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip
          formatter={(value) => [`₹${Number(value).toFixed(2)}`, 'Amount']}
          contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '12px' }}
        />
        <Legend
          wrapperStyle={{ paddingTop: '8px', fontSize: '12px' }}
          iconSize={10}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}

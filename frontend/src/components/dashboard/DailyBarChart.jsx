/**
 * DailyBarChart — bar chart of daily expense totals for a given month.
 *
 * Props:
 *   year     {number}  Calendar year
 *   month    {number}  Month number 1–12
 *   expenses {Array}   Expense objects with `expenseDate` (ISO string) and `amount` (number)
 *
 * Requirements: 7.2
 */
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

/**
 * Returns the number of days in the given year/month.
 */
function daysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

/**
 * Builds an array of { day, total } entries for every day in the month.
 */
function buildDailyTotals(year, month, expenses) {
  const totalDays = daysInMonth(year, month);

  // Initialise every day with 0
  const totals = Array.from({ length: totalDays }, (_, i) => ({
    day: i + 1,
    total: 0,
  }));

  if (!expenses || expenses.length === 0) return totals;

  for (const expense of expenses) {
    if (!expense.expenseDate) continue;
    const date = new Date(expense.expenseDate);
    // Only count expenses that belong to this year/month
    if (date.getFullYear() !== year || date.getMonth() + 1 !== month) continue;
    const dayIndex = date.getDate() - 1;
    totals[dayIndex].total = +(totals[dayIndex].total + Number(expense.amount)).toFixed(2);
  }

  return totals;
}

function DailyBarChart({ year, month, expenses }) {
  const data = buildDailyTotals(year, month, expenses ?? []);

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
        <XAxis
          dataKey="day"
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          label={undefined}
        />
        <YAxis
          tick={{ fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={48}
          tickFormatter={(v) => `₹${v}`}
        />
        <Tooltip
          formatter={(value) => [`₹${Number(value).toFixed(2)}`, 'Amount']}
          labelFormatter={(label) => `Day ${label}`}
        />
        <Bar dataKey="total" fill="#3b82f6" radius={[2, 2, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

export default DailyBarChart;

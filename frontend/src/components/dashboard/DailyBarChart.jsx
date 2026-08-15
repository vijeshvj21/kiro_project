/**
 * DailyBarChart — interactive bar chart of daily expense totals for a given month.
 * Clicking a day bar shows a popup with category pie chart for that day.
 *
 * Props:
 *   year     {number}  Calendar year
 *   month    {number}  Month number 1–12
 *   expenses {Array}   Expense objects with `expenseDate`, `amount`, `categoryName`, `transactionType`
 */
import { useState } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import CategoryPieChart from './CategoryPieChart';

function daysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

function buildDailyTotals(year, month, expenses) {
  const totalDays = daysInMonth(year, month);
  const totals = Array.from({ length: totalDays }, (_, i) => ({
    day: i + 1,
    total: 0,
  }));

  if (!expenses || expenses.length === 0) return totals;

  for (const expense of expenses) {
    if (!expense.expenseDate) continue;
    const date = new Date(expense.expenseDate);
    if (date.getFullYear() !== year || date.getMonth() + 1 !== month) continue;
    // Only count debits for the chart
    if (expense.transactionType === 'CREDIT') continue;
    const dayIndex = date.getDate() - 1;
    totals[dayIndex].total = +(totals[dayIndex].total + Number(expense.amount)).toFixed(2);
  }

  return totals;
}

function getDayBreakdown(year, month, day, expenses) {
  const categoryMap = new Map();
  for (const expense of expenses) {
    if (!expense.expenseDate) continue;
    const date = new Date(expense.expenseDate);
    if (date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) continue;
    if (expense.transactionType === 'CREDIT') continue;
    const cat = expense.categoryName || 'Other';
    categoryMap.set(cat, (categoryMap.get(cat) || 0) + Number(expense.amount));
  }
  return [...categoryMap.entries()]
    .map(([categoryName, total]) => ({ categoryName, total }))
    .sort((a, b) => b.total - a.total);
}

const MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

function DailyBarChart({ year, month, expenses }) {
  const [selectedDay, setSelectedDay] = useState(null);
  const data = buildDailyTotals(year, month, expenses ?? []);

  const handleBarClick = (barData) => {
    if (barData && barData.day) {
      setSelectedDay(barData.day);
    }
  };

  const closePopup = () => setSelectedDay(null);

  const dayBreakdown = selectedDay ? getDayBreakdown(year, month, selectedDay, expenses ?? []) : [];
  const dayTotal = dayBreakdown.reduce((sum, d) => sum + d.total, 0);

  return (
    <>
      <div className="cursor-pointer">
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
            <XAxis
              dataKey="day"
              tick={{ fontSize: 11 }}
              tickLine={false}
              axisLine={false}
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
            <Bar dataKey="total" fill="#3b82f6" radius={[2, 2, 0, 0]} onClick={handleBarClick} cursor="pointer" />
          </BarChart>
        </ResponsiveContainer>
        <p className="text-[10px] text-gray-400 text-center mt-1">Click a day to see category breakdown</p>
      </div>

      {/* Popup modal for day category breakdown */}
      {selectedDay !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm" onClick={closePopup}>
          <div
            className="relative w-full max-w-xl mx-4 rounded-3xl bg-white p-8 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={closePopup}
              className="absolute top-4 right-4 h-8 w-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-500 hover:bg-gray-200 hover:text-gray-700 transition-colors"
            >
              ✕
            </button>

            <div className="mb-6 text-center">
              <h3 className="text-xl font-bold text-gray-900">
                {selectedDay} {MONTH_NAMES[month - 1]} {year}
              </h3>
              <p className="text-sm text-gray-500 mt-1">
                Total: ₹{dayTotal.toFixed(2)}
              </p>
            </div>

            {dayBreakdown.length > 0 ? (
              <>
                <CategoryPieChart data={dayBreakdown} />
                <div className="mt-4 border-t border-gray-100 pt-4">
                  <div className="grid grid-cols-2 gap-2">
                    {dayBreakdown.map(({ categoryName, total }) => (
                      <div key={categoryName} className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2">
                        <span className="text-xs font-medium text-gray-600">{categoryName}</span>
                        <span className="text-xs font-bold text-gray-900">₹{total.toFixed(0)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <div className="py-8 text-center">
                <span className="text-3xl">🎉</span>
                <p className="mt-2 text-sm text-gray-500">No expenses on this day!</p>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

export default DailyBarChart;

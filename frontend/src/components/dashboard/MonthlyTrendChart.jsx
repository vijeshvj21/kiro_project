import { useState, useEffect } from 'react';
import { getMonthlyExpenses } from '../../api/expenses';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import LoadingSpinner from '../common/LoadingSpinner';
import CategoryPieChart from './CategoryPieChart';
import YearNavigator from './YearNavigator';

const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MONTH_FULL = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const BAR_COLORS = ['#6366f1', '#8b5cf6', '#a855f7', '#c084fc', '#818cf8', '#6366f1', '#4f46e5', '#4338ca', '#3730a3', '#312e81', '#6366f1', '#8b5cf6'];

/**
 * MonthlyTrendChart — bar chart of expenses per month.
 * Clicking a month bar opens a popup with the category pie chart for that month.
 */
export default function MonthlyTrendChart() {
  const [year, setYear] = useState(new Date().getFullYear());
  const [data, setData] = useState([]);
  const [monthlyResults, setMonthlyResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [yearTotal, setYearTotal] = useState(0);
  const [selectedMonth, setSelectedMonth] = useState(null); // null or 0-11

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
      const results = await Promise.all(
        months.map((m) => getMonthlyExpenses(year, m))
      );

      setMonthlyResults(results);

      let total = 0;
      const chartData = results.map((result, idx) => {
        const monthTotal = Number(result?.total ?? 0);
        total += monthTotal;
        return {
          month: MONTH_LABELS[idx],
          monthIndex: idx,
          total: monthTotal,
        };
      });

      setData(chartData);
      setYearTotal(total);
    } catch {
      setData([]);
      setMonthlyResults([]);
      setYearTotal(0);
    } finally {
      setLoading(false);
    }
  };

  const handleBarClick = (data) => {
    if (data && data.monthIndex !== undefined) {
      setSelectedMonth(data.monthIndex);
    }
  };

  const closePopup = () => setSelectedMonth(null);

  // Get category breakdown for selected month
  const getSelectedMonthBreakdown = () => {
    if (selectedMonth === null || !monthlyResults[selectedMonth]) return [];
    const breakdown = monthlyResults[selectedMonth]?.categoryBreakdown ?? [];
    return breakdown.map(({ categoryName, total }) => ({ categoryName, total: Number(total) }));
  };

  const currentMonth = new Date().getMonth();

  return (
    <>
      <section className="rounded-2xl border border-gray-100 bg-white/80 backdrop-blur-sm p-6 shadow-lg shadow-gray-100/50">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-gray-800">Monthly Expense Trend</h2>
            <p className="text-xs text-gray-400 mt-0.5">Total: ₹{yearTotal.toFixed(2)} · Click a month to see category breakdown</p>
          </div>
          <YearNavigator year={year} onChange={setYear} />
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data} margin={{ top: 10, right: 10, left: 10, bottom: 5 }} style={{ cursor: 'pointer' }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
              <XAxis
                dataKey="month"
                tick={{ fill: '#64748b', fontSize: 12 }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tick={{ fill: '#64748b', fontSize: 11 }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(v) => `₹${v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v}`}
              />
              <Tooltip
                formatter={(value) => [`₹${Number(value).toFixed(2)}`, 'Spent']}
                contentStyle={{ borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}
                labelStyle={{ fontWeight: 600, color: '#1e293b' }}
              />
              <Bar dataKey="total" radius={[6, 6, 0, 0]} onClick={handleBarClick} cursor="pointer">
                {data.map((entry, index) => (
                  <Cell
                    key={entry.month}
                    fill={index === currentMonth && year === new Date().getFullYear() ? '#4f46e5' : BAR_COLORS[index % BAR_COLORS.length]}
                    opacity={index === currentMonth && year === new Date().getFullYear() ? 1 : 0.7}
                    className="cursor-pointer hover:opacity-100 transition-opacity"
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </section>

      {/* Popup modal for month category breakdown */}
      {selectedMonth !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm" onClick={closePopup}>
          <div
            className="relative w-full max-w-xl mx-4 rounded-3xl bg-white p-8 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close button */}
            <button
              onClick={closePopup}
              className="absolute top-4 right-4 h-8 w-8 rounded-full bg-gray-100 flex items-center justify-center text-gray-500 hover:bg-gray-200 hover:text-gray-700 transition-colors"
            >
              ✕
            </button>

            {/* Header */}
            <div className="mb-6 text-center">
              <h3 className="text-xl font-bold text-gray-900">
                {MONTH_FULL[selectedMonth]} {year}
              </h3>
              <p className="text-sm text-gray-500 mt-1">
                Total: ₹{Number(monthlyResults[selectedMonth]?.total ?? 0).toFixed(2)}
              </p>
            </div>

            {/* Pie Chart */}
            <CategoryPieChart data={getSelectedMonthBreakdown()} />

            {/* Category list */}
            {getSelectedMonthBreakdown().length > 0 && (
              <div className="mt-4 border-t border-gray-100 pt-4">
                <div className="grid grid-cols-2 gap-2">
                  {getSelectedMonthBreakdown().map(({ categoryName, total }) => (
                    <div key={categoryName} className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2">
                      <span className="text-xs font-medium text-gray-600">{categoryName}</span>
                      <span className="text-xs font-bold text-gray-900">₹{total.toFixed(0)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

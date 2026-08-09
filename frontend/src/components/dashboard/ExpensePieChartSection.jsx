import { useState, useEffect } from 'react';
import { getMonthlyExpenses } from '../../api/expenses';
import CategoryPieChart from './CategoryPieChart';
import LoadingSpinner from '../common/LoadingSpinner';

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function getCurrentMonthLabel() {
  const now = new Date();
  return `${MONTH_NAMES[now.getMonth()]} ${now.getFullYear()}`;
}

const PERIODS = [
  { label: getCurrentMonthLabel(), months: 0 }, // Current month only
  { label: '30 days', months: 1 },
  { label: '60 days', months: 2 },
  { label: '90 days', months: 3 },
];

/**
 * ExpensePieChartSection — a self-contained dashboard section that displays
 * a pie chart of expenses broken down by category.
 *
 * Default view shows current month expenses. Users can toggle to 30/60/90 day windows.
 */
export default function ExpensePieChartSection() {
  const [period, setPeriod] = useState(0); // 0 = current month (default)
  const [data, setData] = useState([]);
  const [totalDebit, setTotalDebit] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const now = new Date();
      const promises = [];

      if (period === 0) {
        // Current month only
        promises.push(getMonthlyExpenses(now.getFullYear(), now.getMonth() + 1));
      } else {
        for (let i = 0; i < period; i++) {
          const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
          promises.push(getMonthlyExpenses(date.getFullYear(), date.getMonth() + 1));
        }
      }

      const results = await Promise.all(promises);

      // Aggregate category breakdowns across months
      const categoryMap = new Map();
      let total = 0;

      for (const result of results) {
        total += Number(result?.total ?? 0);
        const breakdown = result?.categoryBreakdown ?? [];
        for (const { categoryName, total: catTotal } of breakdown) {
          categoryMap.set(
            categoryName,
            (categoryMap.get(categoryName) ?? 0) + Number(catTotal),
          );
        }
      }

      const aggregated = [...categoryMap.entries()]
        .map(([categoryName, catTotal]) => ({ categoryName, total: catTotal }))
        .sort((a, b) => b.total - a.total);

      setData(aggregated);
      setTotalDebit(total);
    } catch {
      setData([]);
      setTotalDebit(0);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Expense Breakdown</h2>
        <div className="flex gap-1">
          {PERIODS.map(({ label, months }) => (
            <button
              key={months}
              onClick={() => setPeriod(months)}
              className={`rounded px-3 py-1 text-sm font-medium transition-colors ${
                period === months
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="mb-4">
            <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
              Total Spent
            </p>
            <p className="mt-0.5 text-2xl font-bold text-gray-900">
              ₹{totalDebit.toFixed(2)}
            </p>
          </div>
          <CategoryPieChart data={data} />
        </>
      )}
    </section>
  );
}

/**
 * MonthlySummaryCard — displays the monthly expense summary.
 *
 * Props:
 *   year          {number}    Calendar year
 *   month         {number}    Month number 1–12
 *   onMonthChange {Function}  Called with (year, month) when the user navigates
 *
 * Requirements: 7.1–7.6
 */
import { useEffect, useState } from 'react';
import { getMonthlyExpenses } from '../../api/expenses';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import MonthNavigator from './MonthNavigator';
import DailyBarChart from './DailyBarChart';
import CategoryBreakdownTable from './CategoryBreakdownTable';

/** Empty data shape returned when the API has no data for a month (req 7.5) */
const EMPTY_DATA = {
  totalAmount: 0,
  expenseCount: 0,
  expenses: [],
  categoryBreakdown: [],
};

function MonthlySummaryCard({ year, month, onMonthChange }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function fetchData() {
    setLoading(true);
    setError(null);
    getMonthlyExpenses(year, month)
      .then((res) => {
        setData(res);
      })
      .catch((err) => {
        setError(err?.response?.data?.message ?? err.message ?? 'Failed to load monthly data.');
        setData(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }

  // Re-fetch whenever year or month changes (req 7.1, 7.4)
  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year, month]);

  // Use fetched data or fall back to empty shape for display (req 7.5)
  const displayData = data ?? EMPTY_DATA;
  const { totalAmount, expenseCount, expenses, categoryBreakdown } = displayData;

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      {/* Header: title + month navigator (req 7.4) */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Monthly Summary</h2>
        <MonthNavigator year={year} month={month} onChange={onMonthChange} />
      </div>

      {/* Loading state (req 7.6) */}
      {loading && <LoadingSpinner />}

      {/* Error state with retry (req 7.6) */}
      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchData} />
      )}

      {/* Content (shown when not loading; also shown in empty state per req 7.5) */}
      {!loading && !error && (
        <>
          {/* Summary totals (req 7.1) */}
          <div className="mb-4 flex flex-wrap gap-6">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Total Spent
              </p>
              <p className="mt-0.5 text-2xl font-bold text-gray-900">
                ${Number(totalAmount).toFixed(2)}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Entries
              </p>
              <p className="mt-0.5 text-2xl font-bold text-gray-900">
                {expenseCount}
              </p>
            </div>
          </div>

          {/* Daily bar chart (req 7.2) */}
          <div className="mb-6">
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-gray-500">
              Daily Breakdown
            </p>
            <DailyBarChart year={year} month={month} expenses={expenses} />
          </div>

          {/* Per-category breakdown (req 7.3) */}
          <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-gray-500">
              By Category
            </p>
            <CategoryBreakdownTable breakdown={categoryBreakdown} />
          </div>
        </>
      )}
    </section>
  );
}

export default MonthlySummaryCard;

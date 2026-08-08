import { useEffect, useState, useCallback } from 'react';
import { getWeeklyExpenses } from '../../api/expenses';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import WeekNavigator from './WeekNavigator';
import CategoryBreakdownTable from './CategoryBreakdownTable';

/**
 * WeeklySummaryCard — displays the weekly expense summary for a given ISO week.
 *
 * Props:
 *   year         {number}   ISO year
 *   week         {number}   ISO week number (1–52 or 1–53)
 *   onWeekChange {Function} called with (year, week) when the navigator changes
 *
 * Requirements: 6.1–6.6
 */
function WeeklySummaryCard({ year, week, onWeekChange }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    getWeeklyExpenses(year, week)
      .then((response) => {
        setData(response);
      })
      .catch(() => {
        setError('Failed to load weekly expense data.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [year, week]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /**
   * Derive per-category breakdown from the expenses array since the weekly
   * endpoint does not include a pre-computed categoryBreakdown field.
   * Requirements 6.2: show category name and total for the selected week.
   */
  function buildBreakdown(expenses) {
    if (!expenses || expenses.length === 0) return [];
    const map = {};
    for (const expense of expenses) {
      const name = expense.category?.name ?? 'Unknown';
      map[name] = (map[name] ?? 0) + Number(expense.amount);
    }
    return Object.entries(map)
      .map(([categoryName, total]) => ({ categoryName, total }))
      .sort((a, b) => b.total - a.total);
  }

  const breakdown = data ? buildBreakdown(data.expenses) : [];
  const hasExpenses = data && data.entryCount > 0;

  return (
    <section
      aria-label="Weekly expense summary"
      className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
    >
      {/* Header */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-base font-semibold text-gray-800">Weekly Summary</h2>
      </div>

      {/* Week navigator */}
      <div className="mb-5">
        <WeekNavigator year={year} week={week} onChange={onWeekChange} />
      </div>

      {/* Loading state */}
      {loading && <LoadingSpinner />}

      {/* Error state */}
      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchData} />
      )}

      {/* Data / empty state */}
      {!loading && !error && data && (
        <>
          {/* Summary figures */}
          <div className="mb-5 grid grid-cols-2 gap-4">
            <div className="rounded-md bg-gray-50 p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Total
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                ${Number(data.total ?? 0).toFixed(2)}
              </p>
            </div>
            <div className="rounded-md bg-gray-50 p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Entries
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                {data.entryCount ?? 0}
              </p>
            </div>
          </div>

          {/* Category breakdown */}
          {hasExpenses ? (
            <div>
              <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
                By Category
              </h3>
              <CategoryBreakdownTable breakdown={breakdown} />
            </div>
          ) : (
            <p className="py-4 text-center text-sm text-gray-500">
              No expenses for this week.
            </p>
          )}
        </>
      )}
    </section>
  );
}

export default WeeklySummaryCard;

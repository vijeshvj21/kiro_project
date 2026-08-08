/**
 * YearlySummaryCard — displays the yearly expense summary.
 *
 * Props:
 *   year         {number}    Calendar year
 *   onYearChange {Function}  Called with (year) when the user navigates
 *
 * Requirements: 8.1–8.6
 */
import { useEffect, useState } from 'react';
import { getMonthlyExpenses } from '../../api/expenses';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import YearNavigator from './YearNavigator';
import MonthlyLineChart from './MonthlyLineChart';
import CategoryBreakdownTable from './CategoryBreakdownTable';

/** Months 1–12 */
const ALL_MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

/**
 * Given 12 monthly API responses, aggregate category totals across all months,
 * sort descending by total, and return the top 3.
 */
function buildTop3Categories(monthlyResults) {
  const map = new Map();

  for (const result of monthlyResults) {
    const breakdown = result?.categoryBreakdown ?? [];
    for (const { categoryName, total } of breakdown) {
      map.set(categoryName, (map.get(categoryName) ?? 0) + Number(total));
    }
  }

  return [...map.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([categoryName, total]) => ({ categoryName, total }));
}

function YearlySummaryCard({ year, onYearChange }) {
  const [monthlyResults, setMonthlyResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function fetchData() {
    setLoading(true);
    setError(null);

    // Fetch all 12 months in parallel (req 8.1)
    Promise.all(ALL_MONTHS.map((month) => getMonthlyExpenses(year, month)))
      .then((results) => {
        setMonthlyResults(results);
      })
      .catch((err) => {
        setError(
          err?.response?.data?.message ?? err.message ?? 'Failed to load yearly data.',
        );
        setMonthlyResults([]);
      })
      .finally(() => {
        setLoading(false);
      });
  }

  // Re-fetch whenever year changes (req 8.4)
  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [year]);

  // Derived values ─────────────────────────────────────────────────────────

  /** Sum of all 12 monthly totals (req 8.1) */
  const yearTotal = monthlyResults.reduce(
    (sum, r) => sum + Number(r?.totalAmount ?? 0),
    0,
  );

  /** Total expense entry count across all months (req 8.1) */
  const yearEntryCount = monthlyResults.reduce(
    (sum, r) => sum + Number(r?.expenseCount ?? 0),
    0,
  );

  /**
   * Array of { month, total } for the line chart (req 8.2).
   * Uses 0 for months with no data (req 8.5).
   */
  const monthlyData = ALL_MONTHS.map((month) => {
    const result = monthlyResults[month - 1];
    return {
      month,
      total: Number(result?.totalAmount ?? 0),
    };
  });

  /** Top-3 categories aggregated across all months (req 8.3) */
  const top3Categories = buildTop3Categories(monthlyResults);

  return (
    <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      {/* Header: title + year navigator (req 8.4) */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold text-gray-800">Yearly Summary</h2>
        <YearNavigator year={year} onChange={onYearChange} />
      </div>

      {/* Loading state (req 8.6) */}
      {loading && <LoadingSpinner />}

      {/* Error state with retry (req 8.6) */}
      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchData} />
      )}

      {/* Content (shown when not loading; also shown in empty state per req 8.5) */}
      {!loading && !error && (
        <>
          {/* Summary totals (req 8.1) */}
          <div className="mb-4 flex flex-wrap gap-6">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Total Spent
              </p>
              <p className="mt-0.5 text-2xl font-bold text-gray-900">
                ${yearTotal.toFixed(2)}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                Entries
              </p>
              <p className="mt-0.5 text-2xl font-bold text-gray-900">
                {yearEntryCount}
              </p>
            </div>
          </div>

          {/* Monthly line chart (req 8.2) */}
          <div className="mb-6">
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-gray-500">
              Monthly Trend
            </p>
            <MonthlyLineChart monthlyData={monthlyData} />
          </div>

          {/* Top-3 categories (req 8.3) */}
          <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-gray-500">
              Top Categories
            </p>
            <CategoryBreakdownTable breakdown={top3Categories} />
          </div>
        </>
      )}
    </section>
  );
}

export default YearlySummaryCard;

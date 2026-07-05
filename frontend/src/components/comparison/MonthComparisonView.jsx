import { useState, useEffect, useCallback } from 'react';
import { getMonthlyComparison } from '../../api/expenses';
import MonthNavigator from '../dashboard/MonthNavigator';
import DirectionalIndicator from './DirectionalIndicator';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';

/**
 * MonthComparisonView — month-over-month expense comparison.
 *
 * Props:
 *   year          {number}   Selected year
 *   month         {number}   Selected month (1–12)
 *   onMonthChange {Function} Called with (year, month) when the user navigates
 *
 * Requirements: 9.1–9.7
 */
function MonthComparisonView({ year, month, onMonthChange }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    getMonthlyComparison(year, month)
      .then((result) => {
        setData(result);
      })
      .catch((err) => {
        setError(err?.response?.data?.message || 'Failed to load comparison data.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [year, month]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return (
    <div className="space-y-6">
      {/* Month navigation */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">
          Month-over-Month Comparison
        </h2>
        <MonthNavigator year={year} month={month} onChange={onMonthChange} />
      </div>

      {/* Loading state */}
      {loading && <LoadingSpinner />}

      {/* Error state */}
      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchData} />
      )}

      {/* Data */}
      {!loading && !error && data && (
        <div className="space-y-6">
          {/* Totals row */}
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-lg border border-gray-200 bg-white p-4 text-center shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                This Month
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                ${Number(data.specifiedMonth.total).toFixed(2)}
              </p>
            </div>
            <div className="rounded-lg border border-gray-200 bg-white p-4 text-center shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                Last Month
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                ${Number(data.precedingMonth.total).toFixed(2)}
              </p>
            </div>
          </div>

          {/* Direction + summary stats */}
          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm space-y-3">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-gray-600">Trend:</span>
              <DirectionalIndicator
                specifiedTotal={data.specifiedMonth.total}
                precedingTotal={data.precedingMonth.total}
              />
            </div>

            <p className="text-sm text-gray-700">
              <span className="font-medium">Absolute Difference:</span>{' '}
              ${Number(data.absoluteDifference).toFixed(2)}
            </p>

            <p className="text-sm text-gray-700">
              <span className="font-medium">Change:</span>{' '}
              {data.percentageChangeAvailable
                ? `${Number(data.percentageChange).toFixed(2)}%`
                : 'N/A'}
            </p>
          </div>

          {/* Per-category breakdown */}
          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <h3 className="mb-3 text-sm font-semibold text-gray-700">
              Category Breakdown
            </h3>
            {data.categoryBreakdown && data.categoryBreakdown.length > 0 ? (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                    <th className="pb-2 pr-4">Category</th>
                    <th className="pb-2 pr-4 text-right">This Month</th>
                    <th className="pb-2 text-right">Last Month</th>
                  </tr>
                </thead>
                <tbody>
                  {data.categoryBreakdown.map(
                    ({ categoryName, specifiedMonthTotal, precedingMonthTotal }) => (
                      <tr
                        key={categoryName}
                        className="border-b border-gray-100 last:border-0"
                      >
                        <td className="py-2 pr-4 text-gray-700">{categoryName}</td>
                        <td className="py-2 pr-4 text-right font-medium text-gray-900">
                          ${Number(specifiedMonthTotal).toFixed(2)}
                        </td>
                        <td className="py-2 text-right font-medium text-gray-900">
                          ${Number(precedingMonthTotal).toFixed(2)}
                        </td>
                      </tr>
                    )
                  )}
                </tbody>
              </table>
            ) : (
              <p className="py-4 text-center text-sm text-gray-500">
                No category data available.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default MonthComparisonView;

import { useState, useEffect, useCallback } from 'react';
import { getMonthlyComparison } from '../../api/expenses';
import MonthNavigator from '../dashboard/MonthNavigator';
import DirectionalIndicator from './DirectionalIndicator';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * Returns the previous month's name and year given a month/year.
 */
function getPrevMonth(year, month) {
  if (month === 1) return { name: MONTH_NAMES[11], year: year - 1 };
  return { name: MONTH_NAMES[month - 2], year };
}

function MonthComparisonView({ year, month, onMonthChange }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    getMonthlyComparison(year, month)
      .then((result) => setData(result))
      .catch((err) => setError(err?.response?.data?.message || 'Failed to load comparison data.'))
      .finally(() => setLoading(false));
  }, [year, month]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const currentMonthName = MONTH_NAMES[month - 1];
  const prev = getPrevMonth(year, month);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">Month-over-Month Comparison</h2>
        <MonthNavigator year={year} month={month} onChange={onMonthChange} />
      </div>

      {loading && <LoadingSpinner />}
      {!loading && error && <ErrorMessage message={error} onRetry={fetchData} />}

      {!loading && !error && data && (
        <div className="space-y-6">
          {/* Totals row with actual month names */}
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-lg border border-gray-200 bg-white p-4 text-center shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                This Month ({currentMonthName} {year})
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                ₹{Number(data.specifiedMonth.total).toFixed(2)}
              </p>
            </div>
            <div className="rounded-lg border border-gray-200 bg-white p-4 text-center shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                Last Month ({prev.name} {prev.year})
              </p>
              <p className="mt-1 text-2xl font-bold text-gray-900">
                ₹{Number(data.precedingMonth.total).toFixed(2)}
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
              ₹{Number(data.absoluteDifference).toFixed(2)}
            </p>
            <p className="text-sm text-gray-700">
              <span className="font-medium">Change:</span>{' '}
              {data.percentageChangeAvailable
                ? `${Number(data.percentageChange).toFixed(2)}%`
                : 'N/A'}
            </p>
          </div>

          {/* Per-category breakdown with arrows */}
          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <h3 className="mb-3 text-sm font-semibold text-gray-700">
              Category Breakdown
            </h3>
            {data.categoryBreakdown && data.categoryBreakdown.length > 0 ? (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
                    <th className="pb-2 pr-4">Category</th>
                    <th className="pb-2 pr-4 text-right">This Month ({currentMonthName})</th>
                    <th className="pb-2 pr-4 text-right">Last Month ({prev.name})</th>
                    <th className="pb-2 text-center">Trend</th>
                  </tr>
                </thead>
                <tbody>
                  {data.categoryBreakdown.map(
                    ({ categoryName, specifiedMonthTotal, precedingMonthTotal }) => {
                      const current = Number(specifiedMonthTotal);
                      const previous = Number(precedingMonthTotal);
                      const diff = current - previous;
                      
                      return (
                        <tr
                          key={categoryName}
                          className="border-b border-gray-100 last:border-0"
                        >
                          <td className="py-2 pr-4 text-gray-700 font-medium">{categoryName}</td>
                          <td className="py-2 pr-4 text-right font-medium text-gray-900">
                            ₹{current.toFixed(2)}
                          </td>
                          <td className="py-2 pr-4 text-right font-medium text-gray-500">
                            ₹{previous.toFixed(2)}
                          </td>
                          <td className="py-2 text-center">
                            {diff > 0 && (
                              <span className="inline-flex items-center gap-0.5 text-red-500 font-semibold text-xs">
                                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                  <path fillRule="evenodd" d="M5.293 7.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L10 4.414l-3.293 3.293a1 1 0 01-1.414 0z" clipRule="evenodd"/>
                                </svg>
                                +₹{diff.toFixed(0)}
                              </span>
                            )}
                            {diff < 0 && (
                              <span className="inline-flex items-center gap-0.5 text-green-500 font-semibold text-xs">
                                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                  <path fillRule="evenodd" d="M14.707 12.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L10 15.586l3.293-3.293a1 1 0 011.414 0z" clipRule="evenodd"/>
                                </svg>
                                -₹{Math.abs(diff).toFixed(0)}
                              </span>
                            )}
                            {diff === 0 && (
                              <span className="text-gray-400 text-xs">—</span>
                            )}
                          </td>
                        </tr>
                      );
                    }
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

/**
 * YearComparisonView — year-over-year comparison panel.
 *
 * Fetches GET /api/v1/expenses/comparison/yearly?year={year} and renders:
 *   - YearNavigator to change the selected year
 *   - Side-by-side year totals
 *   - DirectionalIndicator
 *   - Absolute difference
 *   - Percentage change (or "N/A" when unavailable)
 *   - SideBySideBarChart of 12 months × 2 years
 *
 * Props:
 *   year         {number}   Currently selected year
 *   onYearChange {Function} Called with (year) when the user navigates
 *
 * Requirements: 10.1–10.6
 */
import { useEffect, useState, useCallback } from 'react';
import { getYearlyComparison } from '../../api/expenses';
import YearNavigator from '../dashboard/YearNavigator';
import DirectionalIndicator from './DirectionalIndicator';
import SideBySideBarChart from './SideBySideBarChart';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';

function YearComparisonView({ year, onYearChange }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchData = useCallback(() => {
    setLoading(true);
    setError(null);
    getYearlyComparison(year)
      .then((result) => {
        setData(result);
      })
      .catch((err) => {
        setError(err?.response?.data?.message ?? err?.message ?? 'Failed to load yearly comparison.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [year]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const specifiedTotal = data?.specifiedYear?.total ?? 0;
  const precedingTotal = data?.precedingYear?.total ?? 0;
  const specifiedYear  = data?.specifiedYear?.year  ?? year;
  const precedingYear  = data?.precedingYear?.year  ?? year - 1;
  const absoluteDiff   = data?.absoluteDifference   ?? 0;
  const pctChange      = data?.percentageChange;
  const pctAvailable   = data?.percentageChangeAvailable ?? false;
  const monthlyBreakdown = data?.monthlyBreakdown ?? [];

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      {/* Header with navigator */}
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">Year-over-Year Comparison</h2>
        <YearNavigator year={year} onChange={onYearChange} />
      </div>

      {loading && <LoadingSpinner />}

      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchData} />
      )}

      {!loading && !error && data && (
        <>
          {/* Year totals — side by side */}
          <div className="mb-6 grid grid-cols-2 gap-4">
            <div className="rounded-md bg-blue-50 p-4 text-center">
              <p className="text-sm font-medium text-blue-600">{specifiedYear}</p>
              <p className="mt-1 text-2xl font-bold text-blue-800">
                ${Number(specifiedTotal).toFixed(2)}
              </p>
            </div>
            <div className="rounded-md bg-gray-50 p-4 text-center">
              <p className="text-sm font-medium text-gray-500">{precedingYear}</p>
              <p className="mt-1 text-2xl font-bold text-gray-700">
                ${Number(precedingTotal).toFixed(2)}
              </p>
            </div>
          </div>

          {/* Directional indicator + metrics */}
          <div className="mb-6 flex flex-wrap items-center gap-6">
            <DirectionalIndicator
              specifiedTotal={specifiedTotal}
              precedingTotal={precedingTotal}
            />

            <div className="flex items-baseline gap-1">
              <span className="text-sm text-gray-500">Difference:</span>
              <span className="font-semibold text-gray-800">
                {Number(absoluteDiff) >= 0 ? '+' : ''}
                ${Math.abs(Number(absoluteDiff)).toFixed(2)}
              </span>
            </div>

            <div className="flex items-baseline gap-1">
              <span className="text-sm text-gray-500">Change:</span>
              <span className="font-semibold text-gray-800">
                {pctAvailable && pctChange != null
                  ? `${Number(pctChange) >= 0 ? '+' : ''}${Number(pctChange).toFixed(2)}%`
                  : 'N/A'}
              </span>
            </div>
          </div>

          {/* Monthly grouped bar chart */}
          <div>
            <p className="mb-2 text-sm font-medium text-gray-600">Monthly Breakdown</p>
            <SideBySideBarChart
              data={monthlyBreakdown}
              specifiedYear={specifiedYear}
              precedingYear={precedingYear}
            />
          </div>
        </>
      )}
    </div>
  );
}

export default YearComparisonView;

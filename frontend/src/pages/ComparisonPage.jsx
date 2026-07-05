/**
 * ComparisonPage — renders month-over-month and year-over-year comparison views.
 *
 * State:
 *   monthYear — calendar year for the monthly comparison view
 *   monthNum  — month number 1–12 for the monthly comparison view (initialized to current month)
 *   yearNum   — calendar year for the yearly comparison view (initialized to current year)
 *
 * Requirements: 9.1–9.7, 10.1–10.6
 */
import { useState } from 'react';
import MonthComparisonView from '../components/comparison/MonthComparisonView';
import YearComparisonView from '../components/comparison/YearComparisonView';

function ComparisonPage() {
  const now = new Date();

  const [monthYear, setMonthYear] = useState(now.getFullYear());
  const [monthNum, setMonthNum] = useState(now.getMonth() + 1); // getMonth() is 0-indexed
  const [yearNum, setYearNum] = useState(now.getFullYear());

  /** Called by MonthComparisonView when the user navigates to a different month */
  function onMonthChange(year, month) {
    setMonthYear(year);
    setMonthNum(month);
  }

  /** Called by YearComparisonView when the user navigates to a different year */
  function onYearChange(year) {
    setYearNum(year);
  }

  return (
    <div className="space-y-6 p-4 sm:p-6">
      <h1 className="text-2xl font-bold text-gray-900">Expense Comparison</h1>

      {/*
        Responsive layout:
        - Single column on small screens (stacked vertically)
        - Two columns on large screens (side by side)
      */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Month-over-month comparison */}
        <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
          <MonthComparisonView
            year={monthYear}
            month={monthNum}
            onMonthChange={onMonthChange}
          />
        </div>

        {/* Year-over-year comparison */}
        <YearComparisonView
          year={yearNum}
          onYearChange={onYearChange}
        />
      </div>
    </div>
  );
}

export default ComparisonPage;

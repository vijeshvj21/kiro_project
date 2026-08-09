import { useState } from 'react';
import MonthComparisonView from '../components/comparison/MonthComparisonView';
import YearComparisonView from '../components/comparison/YearComparisonView';
import ExpensePieChartSection from '../components/dashboard/ExpensePieChartSection';

function ComparisonPage() {
  const now = new Date();

  const [monthYear, setMonthYear] = useState(now.getFullYear());
  const [monthNum, setMonthNum] = useState(now.getMonth() + 1);
  const [yearNum, setYearNum] = useState(now.getFullYear());

  function onMonthChange(year, month) {
    setMonthYear(year);
    setMonthNum(month);
  }

  function onYearChange(year) {
    setYearNum(year);
  }

  return (
    <div className="space-y-6 p-4 sm:p-6">
      <h1 className="text-2xl font-bold text-gray-900">Expense Comparison</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
          <MonthComparisonView
            year={monthYear}
            month={monthNum}
            onMonthChange={onMonthChange}
          />
        </div>

        <YearComparisonView
          year={yearNum}
          onYearChange={onYearChange}
        />
      </div>

      {/* Category pie chart */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ExpensePieChartSection />
      </div>
    </div>
  );
}

export default ComparisonPage;

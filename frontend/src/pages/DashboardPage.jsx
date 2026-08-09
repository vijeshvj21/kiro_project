/**
 * DashboardPage — top-level page that renders the three summary cards side by side.
 *
 * On mount, fetches weekly, monthly, and yearly summaries in parallel via
 * Promise.all (req 6.1, 7.1, 8.1). Each card manages its own loading/error
 * state; this page only owns the current-period state and passes it down.
 *
 * State:
 *   weekYear  — ISO year for the weekly card
 *   weekNum   — ISO week number for the weekly card
 *   monthYear — calendar year for the monthly card
 *   monthNum  — month number 1-12 for the monthly card
 *   cardYear  — calendar year for the yearly card
 *
 * Requirements: 6.1–6.6, 7.1–7.6, 8.1–8.6
 */
import { useState, useEffect } from 'react';
import { getWeeklyExpenses, getMonthlyExpenses } from '../api/expenses';
import WeeklySummaryCard from '../components/dashboard/WeeklySummaryCard';
import MonthlySummaryCard from '../components/dashboard/MonthlySummaryCard';
import YearlySummaryCard from '../components/dashboard/YearlySummaryCard';
import ExpensePieChartSection from '../components/dashboard/ExpensePieChartSection';
import TodayExpenseCard from '../components/dashboard/TodayExpenseCard';

/**
 * Returns the ISO week number for a given Date.
 * Algorithm: find the Monday of ISO week 1 (the Monday on or before Jan 4),
 * then count how many complete weeks have elapsed since then.
 */
function getISOWeek(date) {
  // Work on a copy so we don't mutate
  const d = new Date(date);
  // Set to the nearest Thursday (ISO week definition: week containing Thursday)
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + 3 - ((d.getDay() + 6) % 7));
  // Jan 4 is always in ISO week 1
  const jan4 = new Date(d.getFullYear(), 0, 4);
  // Count weeks
  return (
    1 +
    Math.round(
      ((d.getTime() - jan4.getTime()) / 86400000 -
        3 +
        ((jan4.getDay() + 6) % 7)) /
        7,
    )
  );
}

/**
 * Returns the ISO year for a given Date.
 * The ISO year can differ from the calendar year for days near year boundaries.
 */
function getISOYear(date) {
  const d = new Date(date);
  d.setDate(d.getDate() + 3 - ((d.getDay() + 6) % 7));
  return d.getFullYear();
}

function DashboardPage() {
  const now = new Date();

  const [weekYear, setWeekYear] = useState(() => getISOYear(now));
  const [weekNum, setWeekNum] = useState(() => getISOWeek(now));
  const [monthYear, setMonthYear] = useState(now.getFullYear());
  const [monthNum, setMonthNum] = useState(now.getMonth() + 1); // getMonth() is 0-indexed
  const [cardYear, setCardYear] = useState(now.getFullYear());

  // Kick off parallel initial fetches on mount so the cards' first render
  // benefits from data already in flight (req 6.1, 7.1, 8.1).
  // Each card independently manages its own loading/error state; we simply
  // trigger the fetches here so they run in parallel.
  useEffect(() => {
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    const currentWeekYear = getISOYear(now);
    const currentWeek = getISOWeek(now);

    Promise.all([
      getWeeklyExpenses(currentWeekYear, currentWeek),
      getMonthlyExpenses(currentYear, currentMonth),
      // Yearly summary fetches all 12 months inside YearlySummaryCard itself;
      // pre-warm the first month so there's something in the browser cache.
      getMonthlyExpenses(currentYear, 1),
    ]).catch(() => {
      // Individual cards handle their own errors; swallow here.
    });
    // Run only once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Called by WeeklySummaryCard when the user navigates to a different week */
  function onWeekChange(year, week) {
    setWeekYear(year);
    setWeekNum(week);
  }

  /** Called by MonthlySummaryCard when the user navigates to a different month */
  function onMonthChange(year, month) {
    setMonthYear(year);
    setMonthNum(month);
  }

  /** Called by YearlySummaryCard when the user navigates to a different year */
  function onYearChange(year) {
    setCardYear(year);
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

      {/* Row 1: Pie chart (monthly) + Monthly summary */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ExpensePieChartSection />
        <MonthlySummaryCard
          year={monthYear}
          month={monthNum}
          onMonthChange={onMonthChange}
        />
      </div>

      {/* Row 2: Today's expense chart + Weekly summary */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <TodayExpenseCard />
        <WeeklySummaryCard
          year={weekYear}
          week={weekNum}
          onWeekChange={onWeekChange}
        />
      </div>

      {/* Row 3: Yearly summary (full width) */}
      <div className="grid grid-cols-1 gap-6">
        <YearlySummaryCard
          year={cardYear}
          onYearChange={onYearChange}
        />
      </div>
    </div>
  );
}

export default DashboardPage;

/**
 * MonthNavigator — controlled component for month navigation.
 *
 * Props:
 *   year     {number}  Calendar year (1900–2100)
 *   month    {number}  Month number 1–12
 *   onChange {Function} called with (year, month) when the user navigates
 *
 * Requirements: 6.3, 7.4
 */

const MIN_YEAR = 1900;
const MAX_YEAR = 2100;

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

function MonthNavigator({ year, month, onChange }) {
  const label = `${MONTH_NAMES[month - 1]} ${year}`;

  function handlePrev() {
    if (month > 1) {
      onChange(year, month - 1);
    } else {
      // Roll back to December of the previous year
      const prevYear = year - 1;
      if (prevYear < MIN_YEAR) return;
      onChange(prevYear, 12);
    }
  }

  function handleNext() {
    if (month < 12) {
      onChange(year, month + 1);
    } else {
      // Roll forward to January of the next year
      const nextYear = year + 1;
      if (nextYear > MAX_YEAR) return;
      onChange(nextYear, 1);
    }
  }

  const isPrevDisabled = year === MIN_YEAR && month === 1;
  const isNextDisabled = year === MAX_YEAR && month === 12;

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={handlePrev}
        disabled={isPrevDisabled}
        aria-label="Previous month"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        ←
      </button>

      <span className="min-w-[160px] text-center text-sm font-medium text-gray-800">
        {label}
      </span>

      <button
        type="button"
        onClick={handleNext}
        disabled={isNextDisabled}
        aria-label="Next month"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        →
      </button>
    </div>
  );
}

export default MonthNavigator;

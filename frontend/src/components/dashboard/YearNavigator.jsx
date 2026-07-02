/**
 * YearNavigator — controlled component for year navigation.
 *
 * Props:
 *   year     {number}  Calendar year (1900–2100)
 *   onChange {Function} called with (year) when the user navigates
 *
 * Requirements: 8.4
 */

const MIN_YEAR = 1900;
const MAX_YEAR = 2100;

function YearNavigator({ year, onChange }) {
  function handlePrev() {
    if (year > MIN_YEAR) {
      onChange(year - 1);
    }
  }

  function handleNext() {
    if (year < MAX_YEAR) {
      onChange(year + 1);
    }
  }

  const isPrevDisabled = year <= MIN_YEAR;
  const isNextDisabled = year >= MAX_YEAR;

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={handlePrev}
        disabled={isPrevDisabled}
        aria-label="Previous year"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        ←
      </button>

      <span className="min-w-[60px] text-center text-sm font-medium text-gray-800">
        {year}
      </span>

      <button
        type="button"
        onClick={handleNext}
        disabled={isNextDisabled}
        aria-label="Next year"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        →
      </button>
    </div>
  );
}

export default YearNavigator;

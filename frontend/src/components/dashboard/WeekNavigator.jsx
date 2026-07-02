/**
 * WeekNavigator — controlled component for ISO week navigation.
 *
 * Props:
 *   year     {number}  ISO year
 *   week     {number}  ISO week number (1–52 or 1–53)
 *   onChange {Function} called with (year, week) when the user navigates
 *
 * Requirements: 6.3, 7.4, 8.4
 */

const MIN_YEAR = 1900;
const MAX_YEAR = 2100;

const MONTH_ABBR = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * Returns true if the given year has 53 ISO weeks.
 * A year has 53 ISO weeks when Jan 1 OR Dec 31 is a Thursday
 * (day-of-week index: 0=Sun, 1=Mon, …, 4=Thu).
 */
function getISOWeeksInYear(year) {
  const jan1 = new Date(year, 0, 1).getDay(); // 0=Sun…6=Sat
  const dec31 = new Date(year, 11, 31).getDay();
  return (jan1 === 4 || dec31 === 4) ? 53 : 52;
}

/**
 * Returns the Monday Date for ISO week `week` of `year`.
 * Uses the standard algorithm: find Jan 4 (always in week 1), then
 * go back to Monday, then advance (week - 1) * 7 days.
 */
function getMondayOfISOWeek(year, week) {
  // Jan 4 is always in ISO week 1
  const jan4 = new Date(year, 0, 4);
  const jan4DayOfWeek = jan4.getDay() || 7; // convert Sun(0) → 7
  const monday = new Date(jan4);
  monday.setDate(jan4.getDate() - (jan4DayOfWeek - 1) + (week - 1) * 7);
  return monday;
}

/** Formats a Date as "dd Mon" (e.g. "07 Jul"). */
function formatDate(date) {
  const day = String(date.getDate()).padStart(2, '0');
  return `${day} ${MONTH_ABBR[date.getMonth()]}`;
}

function WeekNavigator({ year, week, onChange }) {
  const monday = getMondayOfISOWeek(year, week);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);

  const label = `Week ${week}, ${year} (${formatDate(monday)} – ${formatDate(sunday)})`;

  function handlePrev() {
    if (week > 1) {
      onChange(year, week - 1);
    } else {
      // Roll back to last week of previous year
      const prevYear = year - 1;
      if (prevYear < MIN_YEAR) return;
      onChange(prevYear, getISOWeeksInYear(prevYear));
    }
  }

  function handleNext() {
    const weeksInYear = getISOWeeksInYear(year);
    if (week < weeksInYear) {
      onChange(year, week + 1);
    } else {
      // Roll forward to week 1 of next year
      const nextYear = year + 1;
      if (nextYear > MAX_YEAR) return;
      onChange(nextYear, 1);
    }
  }

  const isPrevDisabled = year === MIN_YEAR && week === 1;
  const isNextDisabled = year === MAX_YEAR && week === getISOWeeksInYear(MAX_YEAR);

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={handlePrev}
        disabled={isPrevDisabled}
        aria-label="Previous week"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        ←
      </button>

      <span className="min-w-[260px] text-center text-sm font-medium text-gray-800">
        {label}
      </span>

      <button
        type="button"
        onClick={handleNext}
        disabled={isNextDisabled}
        aria-label="Next week"
        className="rounded px-2 py-1 text-gray-600 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40"
      >
        →
      </button>
    </div>
  );
}

export default WeekNavigator;

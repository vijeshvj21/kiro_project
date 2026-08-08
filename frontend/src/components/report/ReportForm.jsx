import { useState } from 'react';

/**
 * Predefined period constants that map to server-side period keys.
 */
const PREDEFINED_PERIODS = [
  { value: 'CURRENT_WEEK', label: 'Current Week' },
  { value: 'CURRENT_MONTH', label: 'Current Month' },
  { value: 'CURRENT_YEAR', label: 'Current Year' },
  { value: 'LAST_MONTH', label: 'Last Month' },
  { value: 'LAST_YEAR', label: 'Last Year' },
];

/**
 * Form for selecting a report period and format.
 *
 * @param {object}   props
 * @param {Function} props.onGenerate - Called with { startDate, endDate, predefinedPeriod, format }
 *                                      when the form is valid and the user clicks Generate.
 */
function ReportForm({ onGenerate }) {
  const [mode, setMode] = useState('predefined'); // 'predefined' | 'custom'
  const [predefinedPeriod, setPredefinedPeriod] = useState('CURRENT_MONTH');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [format, setFormat] = useState('csv'); // 'csv' | 'pdf'
  const [error, setError] = useState('');

  function handleModeChange(newMode) {
    setMode(newMode);
    setError('');
  }

  function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (mode === 'custom') {
      if (!startDate || !endDate) {
        setError('Both start date and end date are required.');
        return;
      }
      if (startDate > endDate) {
        setError('Start date must not be after end date.');
        return;
      }
      onGenerate({ startDate, endDate, predefinedPeriod: null, format });
    } else {
      onGenerate({ startDate: null, endDate: null, predefinedPeriod, format });
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
      aria-label="Generate report"
    >
      <h2 className="text-base font-semibold text-gray-800">Generate Report</h2>

      {/* Mode toggle */}
      <div className="flex gap-2" role="group" aria-label="Period mode">
        <button
          type="button"
          onClick={() => handleModeChange('predefined')}
          className={`flex-1 rounded-md border px-3 py-2 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue-400 ${
            mode === 'predefined'
              ? 'border-blue-600 bg-blue-600 text-white'
              : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          Predefined Period
        </button>
        <button
          type="button"
          onClick={() => handleModeChange('custom')}
          className={`flex-1 rounded-md border px-3 py-2 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-blue-400 ${
            mode === 'custom'
              ? 'border-blue-600 bg-blue-600 text-white'
              : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          Custom Date Range
        </button>
      </div>

      {/* Predefined period selector */}
      {mode === 'predefined' && (
        <div className="flex flex-col gap-1">
          <label htmlFor="predefined-period" className="text-sm font-medium text-gray-700">
            Period <span aria-hidden="true" className="text-red-500">*</span>
          </label>
          <select
            id="predefined-period"
            value={predefinedPeriod}
            onChange={(e) => setPredefinedPeriod(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-300"
          >
            {PREDEFINED_PERIODS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Custom date range inputs */}
      {mode === 'custom' && (
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1">
            <label htmlFor="start-date" className="text-sm font-medium text-gray-700">
              Start Date <span aria-hidden="true" className="text-red-500">*</span>
            </label>
            <input
              id="start-date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-300"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="end-date" className="text-sm font-medium text-gray-700">
              End Date <span aria-hidden="true" className="text-red-500">*</span>
            </label>
            <input
              id="end-date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-300"
            />
          </div>
        </div>
      )}

      {/* Format selector */}
      <div className="flex flex-col gap-1">
        <span className="text-sm font-medium text-gray-700">Format</span>
        <div className="flex gap-4" role="radiogroup" aria-label="Report format">
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700">
            <input
              type="radio"
              name="format"
              value="csv"
              checked={format === 'csv'}
              onChange={() => setFormat('csv')}
              className="accent-blue-600"
            />
            CSV
          </label>
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700">
            <input
              type="radio"
              name="format"
              value="pdf"
              checked={format === 'pdf'}
              onChange={() => setFormat('pdf')}
              className="accent-blue-600"
            />
            PDF
          </label>
        </div>
      </div>

      {/* Inline validation error */}
      {error && (
        <p
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {error}
        </p>
      )}

      <button
        type="submit"
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        Generate
      </button>
    </form>
  );
}

export default ReportForm;

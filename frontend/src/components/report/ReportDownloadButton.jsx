import { useState } from 'react';
import { generateCsvReport, generatePdfReport } from '../../api/reports';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * Button that triggers a report download by calling the appropriate API endpoint
 * and initiating a browser file download via a temporary Blob URL.
 *
 * @param {object}  props
 * @param {object}  props.reportParams - Parameters from ReportForm: { startDate, endDate, predefinedPeriod, format }
 * @param {boolean} [props.disabled]   - Externally disable the button (e.g. while the form is invalid)
 */
function ReportDownloadButton({ reportParams, disabled }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleDownload() {
    if (!reportParams) return;
    setError('');
    setLoading(true);

    const { format, startDate, endDate, predefinedPeriod } = reportParams;

    // Build the request body — only include fields relevant to the mode
    const requestBody = predefinedPeriod
      ? { predefinedPeriod, format }
      : { startDate, endDate, format };

    try {
      const blob =
        format === 'pdf'
          ? await generatePdfReport(requestBody)
          : await generateCsvReport(requestBody);

      // Derive MIME type and filename
      const mimeType = format === 'pdf' ? 'application/pdf' : 'text/csv';
      const filename = `report.${format}`;

      // Trigger browser download via a temporary object URL
      const url = URL.createObjectURL(new Blob([blob], { type: mimeType }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      URL.revokeObjectURL(url);
    } catch (err) {
      const status = err?.response?.status;
      const apiMessage = err?.response?.data?.message;

      if (status === 400) {
        setError(apiMessage || 'Invalid request. Please check the date range or format.');
      } else if (status === 415) {
        setError(apiMessage || 'Unsupported report format.');
      } else {
        setError('Failed to generate report. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={handleDownload}
        disabled={disabled || loading}
        className="flex items-center justify-center gap-2 rounded-md bg-green-600 px-5 py-2 text-sm font-medium text-white hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:cursor-not-allowed disabled:opacity-50"
        aria-busy={loading}
      >
        {loading ? (
          <>
            {/* Inline mini spinner — reuse the SVG from LoadingSpinner but scaled down */}
            <svg
              className="h-4 w-4 animate-spin"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
              />
            </svg>
            Generating…
          </>
        ) : (
          'Download Report'
        )}
      </button>

      {/* Inline API error */}
      {error && (
        <p
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {error}
        </p>
      )}
    </div>
  );
}

export default ReportDownloadButton;

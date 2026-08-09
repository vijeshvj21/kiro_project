/**
 * Displays a styled error message with an optional retry button and a link to go home.
 *
 * @param {object}   props
 * @param {string}   props.message  - Error text to display
 * @param {Function} [props.onRetry] - If provided, shows a Retry button
 */
function ErrorMessage({ message, onRetry }) {
  if (!message) return null;

  return (
    <div
      role="alert"
      className="flex flex-col gap-3 rounded-md border border-red-300 bg-red-50 p-4 text-red-700"
    >
      <div className="flex items-center gap-3">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5 shrink-0"
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            fillRule="evenodd"
            d="M18 10A8 8 0 11 2 10a8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
            clipRule="evenodd"
          />
        </svg>

        <span className="flex-1 text-sm">{message}</span>
      </div>

      <div className="flex items-center gap-2 ml-8">
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="rounded bg-red-100 px-3 py-1 text-sm font-medium hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-red-400"
          >
            Retry
          </button>
        )}
        <a
          href="/"
          className="rounded bg-gray-100 px-3 py-1 text-sm font-medium text-gray-700 hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-400"
        >
          Go Home
        </a>
      </div>
    </div>
  );
}

export default ErrorMessage;

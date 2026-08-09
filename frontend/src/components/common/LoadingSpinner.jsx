/**
 * Modern loading spinner with pulsing animation.
 */
function LoadingSpinner() {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="relative">
        <div className="h-12 w-12 rounded-full border-4 border-blue-100"></div>
        <div className="absolute left-0 top-0 h-12 w-12 animate-spin rounded-full border-4 border-transparent border-t-indigo-600"></div>
      </div>
      <p className="mt-4 text-sm font-medium text-gray-400 animate-pulse">Loading...</p>
    </div>
  );
}

export default LoadingSpinner;

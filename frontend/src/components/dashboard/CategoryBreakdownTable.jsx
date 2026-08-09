/**
 * CategoryBreakdownTable — reusable table for per-category expense totals.
 *
 * Props:
 *   breakdown {Array<{ categoryName: string, total: number }>}
 *
 * Requirements: 6.2, 7.3, 8.3
 */
function CategoryBreakdownTable({ breakdown }) {
  if (!breakdown || breakdown.length === 0) {
    return (
      <p className="py-4 text-center text-sm text-gray-500">
        No category data available.
      </p>
    );
  }

  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-gray-200 text-left text-xs font-semibold uppercase tracking-wide text-gray-500">
          <th className="pb-2 pr-4">Category</th>
          <th className="pb-2 text-right">Amount</th>
        </tr>
      </thead>
      <tbody>
        {breakdown.map(({ categoryName, total }) => (
          <tr
            key={categoryName}
            className="border-b border-gray-100 last:border-0"
          >
            <td className="py-2 pr-4 text-gray-700">{categoryName}</td>
            <td className="py-2 text-right font-medium text-gray-900">
              ₹{Number(total).toFixed(2)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default CategoryBreakdownTable;

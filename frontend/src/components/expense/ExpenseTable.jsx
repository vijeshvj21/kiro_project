import ExpenseRow from './ExpenseRow';

/**
 * Renders a table of expenses with inline category reassignment.
 *
 * @param {object}   props
 * @param {Array}    props.expenses   - Array of expense objects
 * @param {Function} props.onDelete   - Callback after deletion
 * @param {Array}    props.categories - Array of { id, name } for category dropdown
 * @param {Function} props.onCategoryChange - Callback when category is changed
 */
function ExpenseTable({ expenses, onDelete, categories, onCategoryChange }) {
  if (!expenses || expenses.length === 0) {
    return (
      <p className="py-8 text-center text-sm text-gray-500">
        No expenses found. Add one to get started.
      </p>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 shadow-sm">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Date</th>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Amount</th>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Type</th>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Category</th>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Description</th>
            <th scope="col" className="px-4 py-3 text-left text-sm font-semibold uppercase tracking-wide text-gray-500">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {expenses.map((expense) => (
            <ExpenseRow
              key={expense.id}
              expense={expense}
              onDelete={onDelete}
              categories={categories}
              onCategoryChange={onCategoryChange}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ExpenseTable;

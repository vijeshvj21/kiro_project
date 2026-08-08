import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../common/ConfirmDialog';
import { deleteExpense } from '../../api/expenses';

/**
 * A single row in the expense table.
 *
 * @param {object}   props
 * @param {object}   props.expense  - The expense record
 * @param {Function} props.onDelete - Called with the deleted expense id after successful deletion
 */
function ExpenseRow({ expense, onDelete }) {
  const navigate = useNavigate();
  const [showConfirm, setShowConfirm] = useState(false);

  const formattedAmount = `$${Number(expense.amount).toFixed(2)}`;
  const formattedDate = expense.expenseDate;

  const handleEdit = () => {
    navigate(`/expenses/${expense.id}/edit`);
  };

  const handleDeleteClick = () => {
    setShowConfirm(true);
  };

  const handleConfirmDelete = async () => {
    await deleteExpense(expense.id);
    setShowConfirm(false);
    onDelete(expense.id);
  };

  const handleCancelDelete = () => {
    setShowConfirm(false);
  };

  return (
    <>
      <tr className="border-b border-gray-200 hover:bg-gray-50">
        <td className="px-4 py-3 text-sm text-gray-700">{formattedDate}</td>
        <td className="px-4 py-3 text-sm text-gray-700">{formattedAmount}</td>
        <td className="px-4 py-3 text-sm text-gray-700">
          {expense.categoryName ?? expense.category?.name ?? '—'}
        </td>
        <td className="px-4 py-3 text-sm text-gray-700">
          {expense.description || '—'}
        </td>
        <td className="px-4 py-3 text-sm">
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleEdit}
              className="rounded border border-blue-500 px-3 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-400"
            >
              Edit
            </button>
            <button
              type="button"
              onClick={handleDeleteClick}
              className="rounded border border-red-500 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              Delete
            </button>
          </div>
        </td>
      </tr>

      <ConfirmDialog
        isOpen={showConfirm}
        message="Are you sure you want to delete this expense? This action cannot be undone."
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
      />
    </>
  );
}

export default ExpenseRow;

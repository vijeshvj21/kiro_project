import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../common/ConfirmDialog';
import { deleteExpense } from '../../api/expenses';

// Category colors matching the pie chart
const CATEGORY_COLORS = {
  'Food': '#ef4444',
  'Transport': '#3b82f6',
  'Shopping': '#f59e0b',
  'Entertainment': '#8b5cf6',
  'Healthcare': '#10b981',
  'Utilities': '#06b6d4',
  'Education': '#ec4899',
  'Investment': '#f97316',
  'Other': '#6b7280',
};

function getCategoryColor(categoryName) {
  return CATEGORY_COLORS[categoryName] || '#6b7280';
}

/**
 * A single row in the expense table.
 */
function ExpenseRow({ expense, onDelete }) {
  const navigate = useNavigate();
  const [showConfirm, setShowConfirm] = useState(false);

  const isCredit = expense.transactionType === 'CREDIT';
  const formattedAmount = `₹${Number(expense.amount).toFixed(2)}`;
  const formattedDate = expense.expenseDate;
  const categoryName = expense.categoryName ?? expense.category?.name ?? '—';
  const categoryColor = getCategoryColor(categoryName);

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
        <td className="px-4 py-3 text-base text-gray-700">{formattedDate}</td>
        <td className={`px-4 py-3 text-base font-medium ${isCredit ? 'text-green-600' : 'text-red-600'}`}>
          {formattedAmount}
        </td>
        <td className="px-4 py-3 text-base">
          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
            isCredit 
              ? 'bg-green-100 text-green-700' 
              : 'bg-red-100 text-red-700'
          }`}>
            {isCredit ? 'Credit' : 'Debit'}
          </span>
        </td>
        <td className="px-4 py-3 text-base font-semibold" style={{ color: categoryColor }}>
          {categoryName}
        </td>
        <td className="px-4 py-3 text-base text-gray-700">
          {expense.description || '—'}
        </td>
        <td className="px-4 py-3 text-base">
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

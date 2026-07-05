import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getExpenseById } from '../api/expenses';
import ExpenseForm from '../components/expense/ExpenseForm';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';

/**
 * Page wrapper for creating or editing an expense.
 *
 * - /expenses/new       → create mode (no id param)
 * - /expenses/:id/edit  → edit mode (pre-fills from API)
 */
function ExpenseFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [initialData, setInitialData] = useState(null);
  const [loading, setLoading] = useState(isEdit);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isEdit) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    getExpenseById(id)
      .then((data) => {
        if (!cancelled) setInitialData(data);
      })
      .catch(() => {
        if (!cancelled) setError('Failed to load expense. Please try again.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, isEdit]);

  const handleSave = () => {
    navigate('/expenses');
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">
        {isEdit ? 'Edit Expense' : 'New Expense'}
      </h1>

      {loading && <LoadingSpinner />}

      {!loading && error && <ErrorMessage message={error} />}

      {!loading && !error && (
        <ExpenseForm
          initialData={isEdit ? initialData : null}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      )}
    </div>
  );
}

export default ExpenseFormPage;

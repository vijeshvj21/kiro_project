import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllExpenses } from '../api/expenses';
import ExpenseTable from '../components/expense/ExpenseTable';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';

function ExpenseListPage() {
  const navigate = useNavigate();
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchExpenses = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getAllExpenses();
      setExpenses(data);
    } catch (err) {
      setError('Failed to load expenses. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, []);

  const handleDelete = (deletedId) => {
    setExpenses((prev) => prev.filter((e) => e.id !== deletedId));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Expenses</h1>
        <button
          type="button"
          onClick={() => navigate('/expenses/new')}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          + Add Expense
        </button>
      </div>

      {loading && <LoadingSpinner />}

      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchExpenses} />
      )}

      {!loading && !error && (
        <ExpenseTable expenses={expenses} onDelete={handleDelete} />
      )}
    </div>
  );
}

export default ExpenseListPage;

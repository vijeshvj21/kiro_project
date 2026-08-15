import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllExpenses } from '../api/expenses';
import { getAllCategories } from '../api/categories';
import ExpenseTable from '../components/expense/ExpenseTable';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import GmailPanel from '../components/gmail/GmailPanel';

const PAGE_SIZE = 10;

function ExpenseListPage() {
  const navigate = useNavigate();
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [typeFilter, setTypeFilter] = useState('ALL'); // ALL, DEBIT, CREDIT
  const [categoryFilter, setCategoryFilter] = useState('ALL'); // ALL or category name
  const [currentPage, setCurrentPage] = useState(1);
  const [allCategories, setAllCategories] = useState([]);

  const fetchExpenses = async () => {
    setLoading(true);
    setError(null);
    try {
      const [data, cats] = await Promise.all([getAllExpenses(), getAllCategories()]);
      setExpenses(data);
      setAllCategories(cats);
    } catch (err) {
      setError('Failed to load expenses. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExpenses();
  }, []);

  // Reset page when any filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [typeFilter, categoryFilter]);

  const handleDelete = (deletedId) => {
    setExpenses((prev) => prev.filter((e) => e.id !== deletedId));
  };

  const handleCategoryChange = (expenseId, updatedExpense) => {
    setExpenses((prev) => prev.map((e) => 
      e.id === expenseId ? { ...e, categoryId: updatedExpense.categoryId, categoryName: updatedExpense.categoryName } : e
    ));
  };

  // Get unique categories from expenses for the dropdown
  const categories = [...new Set(expenses.map(e => e.categoryName || 'Other'))].sort();

  // Filter expenses by type and category
  const filteredExpenses = expenses.filter((e) => {
    const typeMatch = typeFilter === 'ALL' || (e.transactionType || 'DEBIT') === typeFilter;
    const catMatch = categoryFilter === 'ALL' || (e.categoryName || 'Other') === categoryFilter;
    return typeMatch && catMatch;
  });

  // Paginate
  const totalPages = Math.ceil(filteredExpenses.length / PAGE_SIZE);
  const paginatedExpenses = filteredExpenses.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  return (
    <div className="space-y-6">
      <GmailPanel onSyncComplete={fetchExpenses} />
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

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Type filter */}
        <div className="flex gap-2">
          {['ALL', 'DEBIT', 'CREDIT'].map((type) => (
            <button
              key={type}
              onClick={() => setTypeFilter(type)}
              className={`rounded-md px-4 py-2 text-sm font-medium transition-colors ${
                typeFilter === type
                  ? type === 'CREDIT'
                    ? 'bg-green-600 text-white'
                    : type === 'DEBIT'
                    ? 'bg-red-600 text-white'
                    : 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {type === 'ALL' ? 'All' : type === 'DEBIT' ? 'Debit' : 'Credit'}
            </button>
          ))}
        </div>

        {/* Category filter */}
        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="ALL">All Categories</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>{cat}</option>
          ))}
        </select>

        <span className="ml-auto self-center text-sm text-gray-500">
          {filteredExpenses.length} entries
        </span>
      </div>

      {loading && <LoadingSpinner />}

      {!loading && error && (
        <ErrorMessage message={error} onRetry={fetchExpenses} />
      )}

      {!loading && !error && (
        <>
          <ExpenseTable expenses={paginatedExpenses} onDelete={handleDelete} categories={allCategories} onCategoryChange={handleCategoryChange} />

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 pt-4">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="text-sm text-gray-600">
                Page {currentPage} of {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default ExpenseListPage;

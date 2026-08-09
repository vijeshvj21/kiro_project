import { useState, useEffect } from 'react';
import { getAllExpenses } from '../../api/expenses';
import CategoryPieChart from './CategoryPieChart';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * TodayExpenseCard — shows today's expenses broken down by category in a pie chart.
 * Displays total spent today and number of transactions.
 */
export default function TodayExpenseCard() {
  const [data, setData] = useState([]);
  const [totalSpent, setTotalSpent] = useState(0);
  const [totalCredit, setTotalCredit] = useState(0);
  const [txCount, setTxCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTodayExpenses();
  }, []);

  const fetchTodayExpenses = async () => {
    setLoading(true);
    try {
      const expenses = await getAllExpenses();
      const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD

      // Filter today's expenses
      const todayExpenses = expenses.filter(e => e.expenseDate === today);

      // Separate debits and credits
      const debits = todayExpenses.filter(e => (e.transactionType || 'DEBIT') === 'DEBIT');
      const credits = todayExpenses.filter(e => e.transactionType === 'CREDIT');

      // Calculate totals
      const debitTotal = debits.reduce((sum, e) => sum + Number(e.amount), 0);
      const creditTotal = credits.reduce((sum, e) => sum + Number(e.amount), 0);

      // Category breakdown (debits only for pie chart)
      const categoryMap = new Map();
      for (const expense of debits) {
        const cat = expense.categoryName || 'Other';
        categoryMap.set(cat, (categoryMap.get(cat) || 0) + Number(expense.amount));
      }

      const breakdown = [...categoryMap.entries()]
        .map(([categoryName, total]) => ({ categoryName, total }))
        .sort((a, b) => b.total - a.total);

      setData(breakdown);
      setTotalSpent(debitTotal);
      setTotalCredit(creditTotal);
      setTxCount(todayExpenses.length);
    } catch {
      setData([]);
      setTotalSpent(0);
      setTotalCredit(0);
      setTxCount(0);
    } finally {
      setLoading(false);
    }
  };

  const today = new Date();
  const dateLabel = today.toLocaleDateString('en-IN', { 
    weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' 
  });

  return (
    <section className="rounded-2xl border border-gray-100 bg-white/80 backdrop-blur-sm p-6 shadow-lg shadow-gray-100/50">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-gray-800">Today's Expenses</h2>
          <p className="text-xs text-gray-400 mt-0.5">{dateLabel}</p>
        </div>
        <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-600">
          {txCount} transactions
        </span>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="mb-4 grid grid-cols-2 gap-3">
            <div className="rounded-lg bg-red-50 p-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-red-400">Spent</p>
              <p className="mt-0.5 text-xl font-bold text-red-600">₹{totalSpent.toFixed(2)}</p>
            </div>
            <div className="rounded-lg bg-green-50 p-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-green-400">Received</p>
              <p className="mt-0.5 text-xl font-bold text-green-600">₹{totalCredit.toFixed(2)}</p>
            </div>
          </div>

          {data.length > 0 ? (
            <CategoryPieChart data={data} />
          ) : (
            <div className="py-8 text-center">
              <span className="text-3xl">🎉</span>
              <p className="mt-2 text-sm text-gray-500">No expenses today!</p>
            </div>
          )}
        </>
      )}
    </section>
  );
}

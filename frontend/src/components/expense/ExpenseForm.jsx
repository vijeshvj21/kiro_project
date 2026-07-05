import { useState, useEffect } from 'react';
import { getAllCategories } from '../../api/categories';
import { createExpense, updateExpense } from '../../api/expenses';
import LoadingSpinner from '../common/LoadingSpinner';

/**
 * Form for creating or editing an expense.
 *
 * @param {object}      props
 * @param {object|null} props.initialData - Expense object for edit mode, or null for create mode
 * @param {Function}    props.onSave      - Called after a successful save
 * @param {Function}    props.onCancel    - Called when the user cancels
 */
function ExpenseForm({ initialData, onSave, onCancel }) {
  const isEdit = Boolean(initialData?.id);

  const [amount, setAmount] = useState(initialData?.amount ?? '');
  const [expenseDate, setExpenseDate] = useState(initialData?.expenseDate ?? '');
  const [categoryId, setCategoryId] = useState(initialData?.category?.id ?? '');
  const [description, setDescription] = useState(initialData?.description ?? '');

  const [categories, setCategories] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [categoriesError, setCategoriesError] = useState(null);

  const [fieldErrors, setFieldErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Load categories on mount
  useEffect(() => {
    let cancelled = false;
    setLoadingCategories(true);
    setCategoriesError(null);
    getAllCategories()
      .then((data) => {
        if (!cancelled) setCategories(data);
      })
      .catch(() => {
        if (!cancelled) setCategoriesError('Failed to load categories.');
      })
      .finally(() => {
        if (!cancelled) setLoadingCategories(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Client-side validation — returns an errors object (empty means valid)
  function validate() {
    const errors = {};

    const numericAmount = parseFloat(amount);
    if (!amount || isNaN(numericAmount) || numericAmount <= 0) {
      errors.amount = 'Amount must be a positive number.';
    }

    if (!expenseDate) {
      errors.expenseDate = 'Date is required.';
    } else {
      const today = new Date().toISOString().slice(0, 10);
      if (expenseDate > today) {
        errors.expenseDate = 'Date cannot be in the future.';
      }
    }

    if (!categoryId) {
      errors.categoryId = 'Please select a category.';
    }

    return errors;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setApiError('');

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    const payload = {
      amount: parseFloat(amount),
      expenseDate,
      categoryId: Number(categoryId),
      description: description.trim() || null,
    };

    setSubmitting(true);
    try {
      if (isEdit) {
        await updateExpense(initialData.id, payload);
      } else {
        await createExpense(payload);
      }
      onSave(payload);
    } catch (err) {
      const apiMessage = err?.response?.data?.message;
      setApiError(apiMessage || 'An unexpected error occurred. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loadingCategories) return <LoadingSpinner />;

  if (categoriesError) {
    return (
      <p
        role="alert"
        className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
      >
        {categoriesError}
      </p>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-lg border border-gray-200 bg-white p-6 shadow-sm"
      aria-label={isEdit ? 'Edit expense' : 'Create expense'}
      noValidate
    >
      {/* Generic API error */}
      {apiError && (
        <p
          role="alert"
          className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {apiError}
        </p>
      )}

      {/* Amount */}
      <div className="flex flex-col gap-1">
        <label htmlFor="expense-amount" className="text-sm font-medium text-gray-700">
          Amount <span aria-hidden="true" className="text-red-500">*</span>
        </label>
        <input
          id="expense-amount"
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0.00"
          aria-invalid={Boolean(fieldErrors.amount)}
          aria-describedby={fieldErrors.amount ? 'amount-error' : undefined}
          className={`rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 ${
            fieldErrors.amount ? 'border-red-400' : 'border-gray-300 focus:border-blue-500'
          }`}
        />
        {fieldErrors.amount && (
          <p id="amount-error" role="alert" className="text-xs text-red-600">
            {fieldErrors.amount}
          </p>
        )}
      </div>

      {/* Date */}
      <div className="flex flex-col gap-1">
        <label htmlFor="expense-date" className="text-sm font-medium text-gray-700">
          Date <span aria-hidden="true" className="text-red-500">*</span>
        </label>
        <input
          id="expense-date"
          type="date"
          value={expenseDate}
          onChange={(e) => setExpenseDate(e.target.value)}
          max={new Date().toISOString().slice(0, 10)}
          aria-invalid={Boolean(fieldErrors.expenseDate)}
          aria-describedby={fieldErrors.expenseDate ? 'date-error' : undefined}
          className={`rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 ${
            fieldErrors.expenseDate ? 'border-red-400' : 'border-gray-300 focus:border-blue-500'
          }`}
        />
        {fieldErrors.expenseDate && (
          <p id="date-error" role="alert" className="text-xs text-red-600">
            {fieldErrors.expenseDate}
          </p>
        )}
      </div>

      {/* Category */}
      <div className="flex flex-col gap-1">
        <label htmlFor="expense-category" className="text-sm font-medium text-gray-700">
          Category <span aria-hidden="true" className="text-red-500">*</span>
        </label>
        <select
          id="expense-category"
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
          aria-invalid={Boolean(fieldErrors.categoryId)}
          aria-describedby={fieldErrors.categoryId ? 'category-error' : undefined}
          className={`rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 ${
            fieldErrors.categoryId ? 'border-red-400' : 'border-gray-300 focus:border-blue-500'
          }`}
        >
          <option value="">-- Select a category --</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </select>
        {fieldErrors.categoryId && (
          <p id="category-error" role="alert" className="text-xs text-red-600">
            {fieldErrors.categoryId}
          </p>
        )}
      </div>

      {/* Description */}
      <div className="flex flex-col gap-1">
        <label htmlFor="expense-description" className="text-sm font-medium text-gray-700">
          Description
        </label>
        <textarea
          id="expense-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          maxLength={500}
          placeholder="Optional notes…"
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-300"
        />
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {submitting ? 'Saving…' : isEdit ? 'Update Expense' : 'Add Expense'}
        </button>
      </div>
    </form>
  );
}

export default ExpenseForm;

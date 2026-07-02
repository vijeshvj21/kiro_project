import { useState, useEffect } from 'react';
import { createCategory, updateCategory } from '../../api/categories';

/**
 * Form for creating or editing a category.
 *
 * @param {object}        props
 * @param {object|null}   props.editingCategory - Category to edit, or null for create
 * @param {Function}      props.onSaved         - Called with the saved category on success
 * @param {Function}      props.onCancel        - Called when the user cancels
 */
function CategoryForm({ editingCategory, onSaved, onCancel }) {
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  // Pre-fill name when editing
  useEffect(() => {
    setName(editingCategory ? editingCategory.name : '');
    setError('');
  }, [editingCategory]);

  const isEdit = editingCategory !== null && editingCategory !== undefined;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    const trimmed = name.trim();
    if (!trimmed) {
      setError('Category name is required.');
      return;
    }

    setSubmitting(true);
    try {
      let saved;
      if (isEdit) {
        saved = await updateCategory(editingCategory.id, { name: trimmed });
      } else {
        saved = await createCategory({ name: trimmed });
      }
      onSaved(saved);
    } catch (err) {
      const status = err?.response?.status;
      const apiMessage = err?.response?.data?.message;

      if (status === 409) {
        setError(apiMessage || 'A category with that name already exists.');
      } else if (status === 400) {
        setError(apiMessage || 'Invalid category name.');
      } else {
        setError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-4 rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
      aria-label={isEdit ? 'Edit category' : 'Create category'}
    >
      <h2 className="text-base font-semibold text-gray-800">
        {isEdit ? 'Edit Category' : 'New Category'}
      </h2>

      {error && (
        <p role="alert" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="flex flex-col gap-1">
        <label htmlFor="category-name" className="text-sm font-medium text-gray-700">
          Name <span aria-hidden="true" className="text-red-500">*</span>
        </label>
        <input
          id="category-name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          required
          autoFocus
          placeholder="e.g. Groceries"
          className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-300"
        />
      </div>

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
          {submitting ? 'Saving…' : 'Save'}
        </button>
      </div>
    </form>
  );
}

export default CategoryForm;

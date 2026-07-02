import { useState } from 'react';
import { deleteCategory } from '../../api/categories';
import ConfirmDialog from '../common/ConfirmDialog';

/**
 * Renders a table of categories with inline Edit and Delete actions per row.
 *
 * @param {object}    props
 * @param {Array}     props.categories       - List of category objects { id, name }
 * @param {Function}  props.onEdit           - Called with a category object when Edit is clicked
 * @param {Function}  props.onDeleted        - Called with the deleted category id on success
 */
function CategoryList({ categories, onEdit, onDeleted }) {
  const [confirmId, setConfirmId] = useState(null);       // id pending confirmation
  const [deletingId, setDeletingId] = useState(null);     // id currently being deleted
  const [rowErrors, setRowErrors] = useState({});         // { [id]: errorMessage }

  function handleDeleteClick(id) {
    // Clear any previous error for this row and open the confirm dialog
    setRowErrors((prev) => ({ ...prev, [id]: undefined }));
    setConfirmId(id);
  }

  async function handleConfirm() {
    const id = confirmId;
    setConfirmId(null);
    setDeletingId(id);
    try {
      await deleteCategory(id);
      onDeleted(id);
    } catch (err) {
      const status = err?.response?.status;
      const apiMessage = err?.response?.data?.message;
      let msg;
      if (status === 409) {
        msg = apiMessage || 'This category is in use and cannot be deleted.';
      } else if (status === 404) {
        msg = 'Category not found.';
      } else {
        msg = 'An unexpected error occurred. Please try again.';
      }
      setRowErrors((prev) => ({ ...prev, [id]: msg }));
    } finally {
      setDeletingId(null);
    }
  }

  const pendingCategory = categories.find((c) => c.id === confirmId);

  if (categories.length === 0) {
    return (
      <p className="py-8 text-center text-sm text-gray-500">
        No categories yet. Add one above.
      </p>
    );
  }

  return (
    <>
      <ConfirmDialog
        isOpen={confirmId !== null}
        message={`Delete category "${pendingCategory?.name ?? ''}"? This cannot be undone.`}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmId(null)}
      />

      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 bg-white text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th
                scope="col"
                className="px-4 py-3 text-left font-semibold text-gray-600"
              >
                Name
              </th>
              <th
                scope="col"
                className="px-4 py-3 text-right font-semibold text-gray-600"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {categories.map((category) => {
              const isDeleting = deletingId === category.id;
              const rowError = rowErrors[category.id];

              return (
                <tr key={category.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className="text-gray-800">{category.name}</span>
                    {rowError && (
                      <p
                        role="alert"
                        className="mt-1 text-xs text-red-600"
                      >
                        {rowError}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="inline-flex gap-2">
                      <button
                        type="button"
                        onClick={() => onEdit(category)}
                        disabled={isDeleting}
                        className="rounded border border-gray-300 px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:opacity-40"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteClick(category.id)}
                        disabled={isDeleting}
                        className="rounded border border-red-300 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-400 disabled:opacity-40"
                      >
                        {isDeleting ? 'Deleting…' : 'Delete'}
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default CategoryList;

import { useState, useEffect } from 'react';
import { getAllCategories } from '../api/categories';
import CategoryForm from '../components/category/CategoryForm';
import CategoryList from '../components/category/CategoryList';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';

function CategoryPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingCategory, setEditingCategory] = useState(null); // null = no edit in progress
  const [showForm, setShowForm] = useState(false);

  async function fetchCategories() {
    setLoading(true);
    setError('');
    try {
      const data = await getAllCategories();
      setCategories(data);
    } catch {
      setError('Failed to load categories. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchCategories();
  }, []);

  // Called by CategoryForm after a successful save
  function handleSaved(savedCategory) {
    if (editingCategory) {
      // Replace the updated category in the list, then re-sort alphabetically
      setCategories((prev) =>
        prev
          .map((c) => (c.id === savedCategory.id ? savedCategory : c))
          .sort((a, b) => a.name.localeCompare(b.name))
      );
    } else {
      // Append the new category and re-sort alphabetically
      setCategories((prev) =>
        [...prev, savedCategory].sort((a, b) => a.name.localeCompare(b.name))
      );
    }
    setShowForm(false);
    setEditingCategory(null);
  }

  // Called by CategoryList after a successful delete
  function handleDeleted(id) {
    setCategories((prev) => prev.filter((c) => c.id !== id));
  }

  // Open form for a new category
  function handleAddClick() {
    setEditingCategory(null);
    setShowForm(true);
  }

  // Open form pre-filled for editing
  function handleEdit(category) {
    setEditingCategory(category);
    setShowForm(true);
  }

  // Close/cancel the form
  function handleCancel() {
    setShowForm(false);
    setEditingCategory(null);
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      {/* Page header */}
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
        {!showForm && (
          <button
            type="button"
            onClick={handleAddClick}
            className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            + Add Category
          </button>
        )}
      </div>

      {/* Create / Edit form */}
      {showForm && (
        <div className="mb-6">
          <CategoryForm
            editingCategory={editingCategory}
            onSaved={handleSaved}
            onCancel={handleCancel}
          />
        </div>
      )}

      {/* List area */}
      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={error} onRetry={fetchCategories} />
      ) : (
        <CategoryList
          categories={categories}
          onEdit={handleEdit}
          onDeleted={handleDeleted}
        />
      )}
    </div>
  );
}

export default CategoryPage;

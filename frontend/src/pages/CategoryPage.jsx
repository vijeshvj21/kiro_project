import { useState, useEffect } from 'react';
import { getAllCategories } from '../api/categories';
import CategoryForm from '../components/category/CategoryForm';
import CategoryList from '../components/category/CategoryList';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';

/**
 * Page for managing expense categories.
 * Shows a form at the top for creating/editing and a list below.
 */
function CategoryPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingCategory, setEditingCategory] = useState(null); // null = create mode

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

  // Called by CategoryForm after a successful save — refresh list and clear edit state
  async function handleSave() {
    setEditingCategory(null);
    await fetchCategories();
  }

  // Called by CategoryList when Edit is clicked — set the category to edit
  function handleEdit(category) {
    setEditingCategory(category);
  }

  // Called by CategoryList after a successful delete — remove from local state
  function handleDelete(id) {
    setCategories((prev) => prev.filter((c) => c.id !== id));
  }

  // Cancel editing (revert form to create mode)
  function handleCancel() {
    setEditingCategory(null);
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Categories</h1>

      {/* Create / Edit form — always visible at top */}
      <div className="mb-6">
        <CategoryForm
          editingCategory={editingCategory}
          onSave={handleSave}
          onCancel={handleCancel}
        />
      </div>

      {/* List area */}
      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message={error} onRetry={fetchCategories} />
      ) : (
        <CategoryList
          categories={categories}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      )}
    </div>
  );
}

export default CategoryPage;

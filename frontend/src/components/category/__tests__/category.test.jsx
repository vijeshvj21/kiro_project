import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import CategoryList from '../CategoryList';
import CategoryForm from '../CategoryForm';

// Mock the categories API module
vi.mock('../../../api/categories', () => ({
  deleteCategory: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
}));

import { deleteCategory, createCategory, updateCategory } from '../../../api/categories';

// ---------------------------------------------------------------------------
// CategoryList tests
// ---------------------------------------------------------------------------
describe('CategoryList', () => {
  const categories = [
    { id: 1, name: 'Groceries' },
    { id: 2, name: 'Entertainment' },
    { id: 3, name: 'Rent' },
  ];

  const onEdit = vi.fn();
  const onDelete = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Requirements 5.2 – categories displayed alphabetically
  it('renders categories in the order provided (alphabetical)', () => {
    const sorted = [...categories].sort((a, b) => a.name.localeCompare(b.name));
    render(<CategoryList categories={sorted} onEdit={onEdit} onDelete={onDelete} />);

    const rows = screen.getAllByRole('row').slice(1); // skip header row
    const renderedNames = rows.map((row) => row.querySelector('span').textContent);

    expect(renderedNames).toEqual(['Entertainment', 'Groceries', 'Rent']);
  });

  // Requirements 5.11 – shows inline 409 error when delete fails (category in use)
  it('shows inline error message when delete fails with 409 conflict', async () => {
    deleteCategory.mockRejectedValueOnce({
      response: {
        status: 409,
        data: { message: 'Category is in use' },
      },
    });

    render(<CategoryList categories={categories} onEdit={onEdit} onDelete={onDelete} />);

    // Click Delete on the first category (Groceries, id=1)
    const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
    fireEvent.click(deleteButtons[0]);

    // Confirm the dialog
    const confirmButton = await screen.findByRole('button', { name: /confirm/i });
    fireEvent.click(confirmButton);

    // Error should appear inline next to the category row
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Category is in use');
    // onDelete should NOT have been called
    expect(onDelete).not.toHaveBeenCalled();
  });

  // Requirements 5.8 – onEdit callback is invoked with the correct category
  it('calls onEdit with the category object when Edit is clicked', () => {
    render(<CategoryList categories={categories} onEdit={onEdit} onDelete={onDelete} />);

    const editButtons = screen.getAllByRole('button', { name: /edit/i });
    fireEvent.click(editButtons[0]); // first category in list

    expect(onEdit).toHaveBeenCalledOnce();
    expect(onEdit).toHaveBeenCalledWith(categories[0]);
  });
});

// ---------------------------------------------------------------------------
// CategoryForm tests
// ---------------------------------------------------------------------------
describe('CategoryForm', () => {
  const onSave = vi.fn();
  const onCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Requirements 5.2 – shows 409 conflict error when create fails with duplicate name
  it('shows conflict error when createCategory rejects with 409', async () => {
    createCategory.mockRejectedValueOnce({
      response: {
        status: 409,
        data: { message: 'Category already exists.' },
      },
    });

    render(<CategoryForm editingCategory={null} onSave={onSave} onCancel={onCancel} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Groceries' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Category already exists.');
    expect(onSave).not.toHaveBeenCalled();
  });

  // Requirements 5.2 – shows 400 validation error when create fails with invalid name
  it('shows validation error when createCategory rejects with 400', async () => {
    createCategory.mockRejectedValueOnce({
      response: {
        status: 400,
        data: { message: 'Invalid category name.' },
      },
    });

    render(<CategoryForm editingCategory={null} onSave={onSave} onCancel={onCancel} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: '!!!' } });
    fireEvent.click(screen.getByRole('button', { name: /save/i }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Invalid category name.');
    expect(onSave).not.toHaveBeenCalled();
  });

  // Requirements 5.8 – in edit mode the name field is pre-filled with editingCategory.name
  it('pre-fills the name field with editingCategory.name in edit mode', () => {
    const editingCategory = { id: 3, name: 'Rent' };
    render(
      <CategoryForm
        editingCategory={editingCategory}
        onSave={onSave}
        onCancel={onCancel}
      />,
    );

    const nameInput = screen.getByLabelText(/name/i);
    expect(nameInput).toHaveValue('Rent');
  });
});

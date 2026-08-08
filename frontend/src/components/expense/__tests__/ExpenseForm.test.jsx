import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ExpenseForm from '../ExpenseForm';

// Mock API modules
vi.mock('../../../api/categories', () => ({
  getAllCategories: vi.fn(),
}));

vi.mock('../../../api/expenses', () => ({
  createExpense: vi.fn(),
  updateExpense: vi.fn(),
}));

import { getAllCategories } from '../../../api/categories';
import { createExpense, updateExpense } from '../../../api/expenses';

const mockCategories = [{ id: 1, name: 'Food' }];

const defaultProps = {
  initialData: null,
  onSave: vi.fn(),
  onCancel: vi.fn(),
};

// Helper: fill the form with valid data and wait for categories to load
async function renderAndFillValid() {
  render(<ExpenseForm {...defaultProps} />);

  // Wait for categories to load (spinner gone, select visible)
  await screen.findByRole('combobox');

  // Amount
  fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '50' } });

  // Date — today's date (never in future)
  const today = new Date().toISOString().slice(0, 10);
  fireEvent.change(screen.getByLabelText(/date/i), { target: { value: today } });

  // Category
  fireEvent.change(screen.getByRole('combobox'), { target: { value: '1' } });
}

describe('ExpenseForm — client-side validation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllCategories.mockResolvedValue(mockCategories);
    createExpense.mockResolvedValue({});
    updateExpense.mockResolvedValue({});
  });

  it('shows an error when amount is empty', async () => {
    render(<ExpenseForm {...defaultProps} />);
    await screen.findByRole('combobox');

    // Leave amount empty, fill the rest validly
    const today = new Date().toISOString().slice(0, 10);
    fireEvent.change(screen.getByLabelText(/date/i), { target: { value: today } });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: /add expense/i }));

    await waitFor(() => {
      expect(screen.getByText(/amount must be a positive number/i)).toBeInTheDocument();
    });
    expect(createExpense).not.toHaveBeenCalled();
  });

  it('shows an error when amount is negative', async () => {
    render(<ExpenseForm {...defaultProps} />);
    await screen.findByRole('combobox');

    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '-10' } });
    const today = new Date().toISOString().slice(0, 10);
    fireEvent.change(screen.getByLabelText(/date/i), { target: { value: today } });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: /add expense/i }));

    await waitFor(() => {
      expect(screen.getByText(/amount must be a positive number/i)).toBeInTheDocument();
    });
    expect(createExpense).not.toHaveBeenCalled();
  });

  it('shows an error when no category is selected', async () => {
    render(<ExpenseForm {...defaultProps} />);
    await screen.findByRole('combobox');

    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '25' } });
    const today = new Date().toISOString().slice(0, 10);
    fireEvent.change(screen.getByLabelText(/date/i), { target: { value: today } });
    // Leave category at default "-- Select a category --"

    fireEvent.click(screen.getByRole('button', { name: /add expense/i }));

    await waitFor(() => {
      expect(screen.getByText(/please select a category/i)).toBeInTheDocument();
    });
    expect(createExpense).not.toHaveBeenCalled();
  });

  it('shows an error when date is in the future', async () => {
    render(<ExpenseForm {...defaultProps} />);
    await screen.findByRole('combobox');

    fireEvent.change(screen.getByLabelText(/amount/i), { target: { value: '25' } });

    // A date clearly in the future
    const futureDate = '2099-12-31';
    fireEvent.change(screen.getByLabelText(/date/i), { target: { value: futureDate } });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '1' } });

    fireEvent.click(screen.getByRole('button', { name: /add expense/i }));

    await waitFor(() => {
      expect(screen.getByText(/date cannot be in the future/i)).toBeInTheDocument();
    });
    expect(createExpense).not.toHaveBeenCalled();
  });
});

describe('ExpenseForm — valid create submission', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllCategories.mockResolvedValue(mockCategories);
    createExpense.mockResolvedValue({});
    updateExpense.mockResolvedValue({});
  });

  it('calls createExpense with the correct payload on valid submission', async () => {
    await renderAndFillValid();

    const today = new Date().toISOString().slice(0, 10);

    fireEvent.click(screen.getByRole('button', { name: /add expense/i }));

    await waitFor(() => {
      expect(createExpense).toHaveBeenCalledTimes(1);
      expect(createExpense).toHaveBeenCalledWith({
        amount: 50,
        expenseDate: today,
        categoryId: 1,
        description: null,
      });
    });
    expect(updateExpense).not.toHaveBeenCalled();
  });
});

describe('ExpenseForm — edit mode', () => {
  const initialData = {
    id: 42,
    amount: 123.45,
    expenseDate: '2024-06-15',
    category: { id: 1, name: 'Food' },
    description: 'Lunch',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    getAllCategories.mockResolvedValue(mockCategories);
    createExpense.mockResolvedValue({});
    updateExpense.mockResolvedValue({});
  });

  it('pre-fills amount, date, and description from initialData', async () => {
    render(<ExpenseForm initialData={initialData} onSave={vi.fn()} onCancel={vi.fn()} />);
    await screen.findByRole('combobox');

    expect(screen.getByLabelText(/amount/i)).toHaveValue(123.45);
    expect(screen.getByLabelText(/date/i)).toHaveValue('2024-06-15');
    expect(screen.getByLabelText(/description/i)).toHaveValue('Lunch');
  });

  it('calls updateExpense instead of createExpense on submission', async () => {
    render(<ExpenseForm initialData={initialData} onSave={vi.fn()} onCancel={vi.fn()} />);
    await screen.findByRole('combobox');

    fireEvent.click(screen.getByRole('button', { name: /update expense/i }));

    await waitFor(() => {
      expect(updateExpense).toHaveBeenCalledTimes(1);
      expect(updateExpense).toHaveBeenCalledWith(42, {
        amount: 123.45,
        expenseDate: '2024-06-15',
        categoryId: 1,
        description: 'Lunch',
      });
    });
    expect(createExpense).not.toHaveBeenCalled();
  });
});

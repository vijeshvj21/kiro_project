import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ReportForm from '../ReportForm';

describe('ReportForm — default state', () => {
  it('defaults to predefined mode with CURRENT_MONTH selected', () => {
    const onGenerate = vi.fn();
    render(<ReportForm onGenerate={onGenerate} />);

    // "Predefined Period" button should be visible (mode toggle present)
    expect(screen.getByRole('button', { name: /predefined period/i })).toBeInTheDocument();

    // The predefined period dropdown should be visible with CURRENT_MONTH selected
    const select = screen.getByRole('combobox', { name: /period/i });
    expect(select).toBeInTheDocument();
    expect(select).toHaveValue('CURRENT_MONTH');

    // Custom date inputs should NOT be present
    expect(screen.queryByLabelText(/start date/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/end date/i)).not.toBeInTheDocument();
  });
});

describe('ReportForm — predefined mode submission', () => {
  let onGenerate;

  beforeEach(() => {
    onGenerate = vi.fn();
  });

  it('calls onGenerate with predefinedPeriod and null dates when submitted in predefined mode', () => {
    render(<ReportForm onGenerate={onGenerate} />);

    fireEvent.click(screen.getByRole('button', { name: /generate/i }));

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(onGenerate).toHaveBeenCalledWith({
      predefinedPeriod: 'CURRENT_MONTH',
      startDate: null,
      endDate: null,
      format: 'csv',
    });
  });

  it('passes the updated predefined period value after changing the dropdown', () => {
    render(<ReportForm onGenerate={onGenerate} />);

    const select = screen.getByRole('combobox', { name: /period/i });
    fireEvent.change(select, { target: { value: 'LAST_MONTH' } });

    fireEvent.click(screen.getByRole('button', { name: /generate/i }));

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(onGenerate).toHaveBeenCalledWith({
      predefinedPeriod: 'LAST_MONTH',
      startDate: null,
      endDate: null,
      format: 'csv',
    });
  });
});

describe('ReportForm — custom date mode validation', () => {
  let onGenerate;

  beforeEach(() => {
    onGenerate = vi.fn();
  });

  function switchToCustomMode() {
    fireEvent.click(screen.getByRole('button', { name: /custom date range/i }));
  }

  it('shows a validation error and does NOT call onGenerate when startDate is after endDate', () => {
    render(<ReportForm onGenerate={onGenerate} />);
    switchToCustomMode();

    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: '2024-06-15' } });
    fireEvent.change(screen.getByLabelText(/end date/i), { target: { value: '2024-06-01' } });

    fireEvent.click(screen.getByRole('button', { name: /generate/i }));

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/start date must not be after end date/i);
    expect(onGenerate).not.toHaveBeenCalled();
  });

  it('calls onGenerate with correct payload when custom dates are valid', () => {
    render(<ReportForm onGenerate={onGenerate} />);
    switchToCustomMode();

    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: '2024-05-01' } });
    fireEvent.change(screen.getByLabelText(/end date/i), { target: { value: '2024-05-31' } });

    fireEvent.click(screen.getByRole('button', { name: /generate/i }));

    expect(onGenerate).toHaveBeenCalledTimes(1);
    expect(onGenerate).toHaveBeenCalledWith({
      startDate: '2024-05-01',
      endDate: '2024-05-31',
      predefinedPeriod: null,
      format: 'csv',
    });
  });
});

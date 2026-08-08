/**
 * Tests for WeekNavigator, MonthNavigator, and YearNavigator components.
 * Requirements: 6.3, 7.4, 8.4
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import WeekNavigator from '../WeekNavigator';
import MonthNavigator from '../MonthNavigator';
import YearNavigator from '../YearNavigator';

// ---------------------------------------------------------------------------
// WeekNavigator
// ---------------------------------------------------------------------------
describe('WeekNavigator', () => {
  it('renders the current week label', () => {
    render(<WeekNavigator year={2025} week={1} onChange={vi.fn()} />);
    // The label includes the year and week number; the exact date range may vary
    // by environment timezone, so just verify the structural parts.
    expect(screen.getByText(/Week 1, 2025/)).toBeInTheDocument();
    // Also verify month abbreviations appear for Dec and Jan (cross-year week)
    expect(screen.getByText(/Dec.*Jan|Jan.*Dec/)).toBeInTheDocument();
  });

  it('clicking Prev decrements the week', () => {
    const onChange = vi.fn();
    render(<WeekNavigator year={2025} week={10} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /previous week/i }));
    expect(onChange).toHaveBeenCalledWith(2025, 9);
  });

  it('clicking Next increments the week', () => {
    const onChange = vi.fn();
    render(<WeekNavigator year={2025} week={10} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /next week/i }));
    expect(onChange).toHaveBeenCalledWith(2025, 11);
  });

  it('at week 1, clicking Prev rolls back to last week of previous year', () => {
    // 2021 starts at week 1 — 2020 has 53 ISO weeks
    const onChange = vi.fn();
    render(<WeekNavigator year={2021} week={1} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /previous week/i }));
    expect(onChange).toHaveBeenCalledWith(2020, 53);
  });

  it('at last week of year, clicking Next rolls forward to week 1 of next year', () => {
    // 2021 has 52 ISO weeks
    const onChange = vi.fn();
    render(<WeekNavigator year={2021} week={52} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /next week/i }));
    expect(onChange).toHaveBeenCalledWith(2022, 1);
  });

  it('Prev is disabled at the minimum boundary (year=1900, week=1)', () => {
    render(<WeekNavigator year={1900} week={1} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /previous week/i })).toBeDisabled();
  });
});

// ---------------------------------------------------------------------------
// MonthNavigator
// ---------------------------------------------------------------------------
describe('MonthNavigator', () => {
  it('renders the current month name and year', () => {
    render(<MonthNavigator year={2024} month={7} onChange={vi.fn()} />);
    expect(screen.getByText('July 2024')).toBeInTheDocument();
  });

  it('clicking Prev decrements the month', () => {
    const onChange = vi.fn();
    render(<MonthNavigator year={2024} month={7} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /previous month/i }));
    expect(onChange).toHaveBeenCalledWith(2024, 6);
  });

  it('clicking Next increments the month', () => {
    const onChange = vi.fn();
    render(<MonthNavigator year={2024} month={7} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /next month/i }));
    expect(onChange).toHaveBeenCalledWith(2024, 8);
  });

  it('at January, clicking Prev rolls to December of previous year', () => {
    const onChange = vi.fn();
    render(<MonthNavigator year={2024} month={1} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /previous month/i }));
    expect(onChange).toHaveBeenCalledWith(2023, 12);
  });

  it('at December, clicking Next rolls to January of next year', () => {
    const onChange = vi.fn();
    render(<MonthNavigator year={2024} month={12} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /next month/i }));
    expect(onChange).toHaveBeenCalledWith(2025, 1);
  });

  it('Prev is disabled at year=1900, month=1', () => {
    render(<MonthNavigator year={1900} month={1} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /previous month/i })).toBeDisabled();
  });

  it('Next is disabled at year=2100, month=12', () => {
    render(<MonthNavigator year={2100} month={12} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /next month/i })).toBeDisabled();
  });
});

// ---------------------------------------------------------------------------
// YearNavigator
// ---------------------------------------------------------------------------
describe('YearNavigator', () => {
  it('renders the current year', () => {
    render(<YearNavigator year={2024} onChange={vi.fn()} />);
    expect(screen.getByText('2024')).toBeInTheDocument();
  });

  it('clicking Prev decrements the year', () => {
    const onChange = vi.fn();
    render(<YearNavigator year={2024} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /previous year/i }));
    expect(onChange).toHaveBeenCalledWith(2023);
  });

  it('clicking Next increments the year', () => {
    const onChange = vi.fn();
    render(<YearNavigator year={2024} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: /next year/i }));
    expect(onChange).toHaveBeenCalledWith(2025);
  });

  it('Prev is disabled at year=1900', () => {
    render(<YearNavigator year={1900} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /previous year/i })).toBeDisabled();
  });

  it('Next is disabled at year=2100', () => {
    render(<YearNavigator year={2100} onChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /next year/i })).toBeDisabled();
  });
});

import client from './client';

const BASE = '/api/v1/expenses';

/** Create a new expense */
export const createExpense = (data) => client.post(BASE, data).then((r) => r.data);

/** List all expenses (sorted by date desc, max 1000) */
export const getAllExpenses = () => client.get(BASE).then((r) => r.data);

/** Get a single expense by ID */
export const getExpenseById = (id) => client.get(`${BASE}/${id}`).then((r) => r.data);

/** Update an existing expense */
export const updateExpense = (id, data) =>
  client.put(`${BASE}/${id}`, data).then((r) => r.data);

/** Delete an expense */
export const deleteExpense = (id) => client.delete(`${BASE}/${id}`);

/** Get weekly expense summary */
export const getWeeklyExpenses = (year, week) =>
  client.get(`${BASE}/weekly`, { params: { year, week } }).then((r) => r.data);

/** Get monthly expense summary */
export const getMonthlyExpenses = (year, month) =>
  client.get(`${BASE}/monthly`, { params: { year, month } }).then((r) => r.data);

/** Get month-over-month comparison */
export const getMonthlyComparison = (year, month) =>
  client
    .get(`${BASE}/comparison/monthly`, { params: { year, month } })
    .then((r) => r.data);

/** Get year-over-year comparison */
export const getYearlyComparison = (year) =>
  client
    .get(`${BASE}/comparison/yearly`, { params: { year } })
    .then((r) => r.data);

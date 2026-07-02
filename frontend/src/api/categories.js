import client from './client';

const BASE = '/api/v1/categories';

/** List all categories (alphabetical) */
export const getAllCategories = () => client.get(BASE).then((r) => r.data);

/** Create a new category */
export const createCategory = (data) => client.post(BASE, data).then((r) => r.data);

/** Update a category name */
export const updateCategory = (id, data) =>
  client.put(`${BASE}/${id}`, data).then((r) => r.data);

/** Delete a category */
export const deleteCategory = (id) => client.delete(`${BASE}/${id}`);

import client from './client';

const BASE = '/api/v1/reports';

/** Generate and download a CSV report (returns a Blob) */
export const generateCsvReport = (data) =>
  client
    .post(`${BASE}/csv`, data, { responseType: 'blob' })
    .then((r) => r.data);

/** Generate and download a PDF report (returns a Blob) */
export const generatePdfReport = (data) =>
  client
    .post(`${BASE}/pdf`, data, { responseType: 'blob' })
    .then((r) => r.data);

import axios from 'axios';

const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor: pass through 2xx, throw enriched error for non-2xx
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const enriched = new Error(
      error.response?.data?.message || error.message || 'An unexpected error occurred'
    );
    enriched.status = error.response?.status;
    enriched.data = error.response?.data;
    return Promise.reject(enriched);
  }
);

export default client;

import client from './client';

/**
 * Get Gmail connection status.
 * @returns {Promise<{connected: boolean}>}
 */
export const getConnectionStatus = () =>
  client.get('/api/v1/email/status').then(res => res.data);

/**
 * Get Google OAuth2 authorization URL.
 * @returns {Promise<{authUrl: string}>}
 */
export const getAuthUrl = () =>
  client.get('/api/v1/email/auth').then(res => res.data);

/**
 * Disconnect Gmail account.
 * @returns {Promise<{message: string, warning: string|null}>}
 */
export const disconnect = () =>
  client.post('/api/v1/email/disconnect').then(res => res.data);

/**
 * Trigger a manual sync.
 * @returns {Promise<{importedCount: number}>}
 */
export const triggerSync = () =>
  client.post('/api/v1/email/sync').then(res => res.data);

/**
 * Get the last sync status.
 * @returns {Promise<{lastSyncAt: string|null, importedCount: number}>}
 */
export const getSyncStatus = () =>
  client.get('/api/v1/email/sync/status').then(res => res.data);

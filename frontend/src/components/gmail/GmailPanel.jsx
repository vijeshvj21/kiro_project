import { useState, useEffect } from 'react';
import { getConnectionStatus, getAuthUrl, getSyncStatus, triggerSync, disconnect } from '../../api/email';

export default function GmailPanel({ onSyncComplete }) {
  const [state, setState] = useState('loading');
  const [syncStatus, setSyncStatus] = useState({ lastSyncAt: null, importedCount: 0 });
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetchStatus();
  }, []);

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  const fetchStatus = async () => {
    try {
      const { connected } = await getConnectionStatus();
      if (connected) {
        setState('connected');
        const status = await getSyncStatus();
        setSyncStatus(status);
      } else {
        setState('disconnected');
      }
    } catch (err) {
      setState('disconnected');
    }
  };

  const handleConnect = async () => {
    try {
      const { authUrl } = await getAuthUrl();
      window.location.href = authUrl;
    } catch (err) {
      setError('Failed to initiate Gmail connection');
    }
  };

  const handleSync = async () => {
    setState('syncing');
    setError(null);
    try {
      const { importedCount } = await triggerSync();
      const status = await getSyncStatus();
      setSyncStatus(status);
      setState('connected');
      if (importedCount > 0) {
        setToast(`Imported ${importedCount} new expense${importedCount !== 1 ? 's' : ''}`);
      } else {
        setToast('No new expenses found');
      }
      // Notify parent to refresh expense list
      if (onSyncComplete) onSyncComplete();
    } catch (err) {
      setState('connected');
      setError(err.message || 'Sync failed. Please try again.');
    }
  };

  const handleDisconnect = async () => {
    try {
      await disconnect();
      setState('disconnected');
      setSyncStatus({ lastSyncAt: null, importedCount: 0 });
      setToast('Gmail disconnected successfully');
    } catch (err) {
      setError(err.message || 'Failed to disconnect. Please try again.');
    }
  };

  const formatLastSync = (timestamp) => {
    if (!timestamp) return 'Never synced';
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins !== 1 ? 's' : ''} ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hour${diffHours !== 1 ? 's' : ''} ago`;
    return date.toLocaleDateString();
  };

  if (state === 'loading') {
    return (
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <p className="text-gray-500">Loading Gmail status...</p>
      </div>
    );
  }

  return (
    <>
      {/* Full-screen overlay during sync */}
      {state === 'syncing' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-xl p-8 shadow-2xl flex flex-col items-center gap-4 max-w-sm mx-4">
            <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-200 border-t-blue-600"></div>
            <p className="text-lg font-medium text-gray-800">Syncing your emails...</p>
            <p className="text-sm text-gray-500 text-center">
              Please wait while your expense data is fetched from Gmail. This may take a moment.
            </p>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <h3 className="text-lg font-semibold mb-2 flex items-center gap-2">
          <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 4H4C2.9 4 2 4.9 2 6V18C2 19.1 2.9 20 4 20H20C21.1 20 22 19.1 22 18V6C22 4.9 21.1 4 20 4ZM20 8L12 13L4 8V6L12 11L20 6V8Z" fill="currentColor"/>
          </svg>
          Gmail Import
        </h3>

        {toast && (
          <div className="bg-green-50 text-green-700 px-3 py-2 rounded mb-3 text-sm">
            {toast}
          </div>
        )}

        {error && (
          <div className="bg-red-50 text-red-700 px-3 py-2 rounded mb-3 text-sm">
            {error}
            <button onClick={() => setError(null)} className="ml-2 underline">Dismiss</button>
          </div>
        )}

        {state === 'disconnected' && (
          <div>
            <p className="text-gray-600 text-sm mb-3">
              Connect your Gmail to auto-import expenses from bank emails.
            </p>
            <button
              onClick={handleConnect}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors"
            >
              Connect Gmail
            </button>
          </div>
        )}

        {(state === 'connected' || state === 'syncing') && (
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-block w-2 h-2 bg-green-500 rounded-full"></span>
              <span className="text-green-700 text-sm font-medium">Gmail connected</span>
            </div>
            <p className="text-gray-500 text-sm mb-3">
              Last sync: {formatLastSync(syncStatus.lastSyncAt)}
              {syncStatus.lastSyncAt && ` (${syncStatus.importedCount} imported)`}
            </p>
            <div className="flex gap-2">
              <button
                onClick={handleSync}
                disabled={state === 'syncing'}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {state === 'syncing' ? 'Syncing...' : 'Sync from Gmail'}
              </button>
              <button
                onClick={handleDisconnect}
                disabled={state === 'syncing'}
                className="bg-gray-200 text-gray-700 px-4 py-2 rounded hover:bg-gray-300 transition-colors disabled:opacity-50"
              >
                Disconnect
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

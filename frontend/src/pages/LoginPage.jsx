import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Premium login page — light, clean, elegant design with soft gradients.
 */
function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }

    setLoading(true);
    setTimeout(() => {
      localStorage.setItem('spendwise_auth', 'true');
      localStorage.setItem('spendwise_user', email);
      setLoading(false);
      navigate('/');
    }, 800);
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-to-br from-white via-blue-50 to-indigo-100">
      {/* Subtle decorative shapes */}
      <div className="absolute top-0 right-0 h-[500px] w-[500px] rounded-full bg-gradient-to-bl from-indigo-200/40 to-purple-100/30 blur-3xl -translate-y-1/3 translate-x-1/4"></div>
      <div className="absolute bottom-0 left-0 h-[400px] w-[400px] rounded-full bg-gradient-to-tr from-blue-200/40 to-cyan-100/30 blur-3xl translate-y-1/3 -translate-x-1/4"></div>
      <div className="absolute top-1/2 left-1/2 h-[300px] w-[300px] rounded-full bg-purple-100/20 blur-2xl -translate-x-1/2 -translate-y-1/2"></div>

      {/* Content */}
      <div className="relative z-10 w-full max-w-[420px] mx-4">
        {/* Logo + Brand */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center h-16 w-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 shadow-xl shadow-indigo-200/60 mb-5">
            <svg viewBox="0 0 32 32" className="h-9 w-9">
              <text x="16" y="23" fontSize="18" textAnchor="middle" fill="white" fontFamily="system-ui" fontWeight="bold">₹</text>
            </svg>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900">
            SpendWise <span className="bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">Pro</span>
          </h1>
          <p className="mt-2 text-sm text-gray-500">
            Your personal finance companion
          </p>
        </div>

        {/* Card */}
        <div className="rounded-3xl border border-gray-100 bg-white/70 backdrop-blur-xl p-8 shadow-xl shadow-gray-200/40">
          <h2 className="text-lg font-semibold text-gray-800 mb-1">Sign in to your account</h2>
          <p className="text-xs text-gray-400 mb-6">Enter your credentials to continue</p>

          {error && (
            <div className="mb-4 rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="email" className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="w-full rounded-xl border border-gray-200 bg-gray-50/50 px-4 py-3 text-sm text-gray-900 placeholder-gray-300 focus:border-indigo-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-indigo-100 transition-all"
                autoComplete="email"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-xl border border-gray-200 bg-gray-50/50 px-4 py-3 text-sm text-gray-900 placeholder-gray-300 focus:border-indigo-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-indigo-100 transition-all"
                autoComplete="current-password"
              />
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-sm text-gray-500">
                <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                Remember me
              </label>
              <a href="#" className="text-sm font-medium text-indigo-600 hover:text-indigo-500 transition-colors">
                Forgot password?
              </a>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-3.5 text-sm font-bold text-white shadow-lg shadow-indigo-200/50 transition-all duration-200 hover:from-indigo-500 hover:to-purple-500 hover:shadow-xl hover:shadow-indigo-300/50 hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:translate-y-0"
            >
              {loading ? (
                <span className="inline-flex items-center gap-2">
                  <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" className="opacity-25"/>
                    <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" className="opacity-75"/>
                  </svg>
                  Signing in...
                </span>
              ) : 'Sign In →'}
            </button>
          </form>

          {/* Divider */}
          <div className="my-6 flex items-center gap-4">
            <div className="flex-1 border-t border-gray-100"></div>
            <span className="text-xs text-gray-300 font-medium">OR</span>
            <div className="flex-1 border-t border-gray-100"></div>
          </div>

          {/* Google login */}
          <button
            type="button"
            onClick={() => { localStorage.setItem('spendwise_auth', 'true'); navigate('/'); }}
            className="w-full flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-3 text-sm font-semibold text-gray-700 shadow-sm transition-all hover:bg-gray-50 hover:shadow-md hover:-translate-y-0.5 active:translate-y-0"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Continue with Google
          </button>
        </div>

        {/* Footer */}
        <p className="mt-8 text-center text-xs text-gray-400">
          © 2026 SpendWise Pro · Privacy · Terms
        </p>
      </div>
    </div>
  );
}

export default LoginPage;

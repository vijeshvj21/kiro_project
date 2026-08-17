import { NavLink, useNavigate } from 'react-router-dom';

const navItems = [
  { to: '/',           label: 'Dashboard',  icon: '◉' },
  { to: '/expenses',   label: 'Expenses',   icon: '◈' },
  { to: '/categories', label: 'Categories', icon: '◆' },
  { to: '/comparison', label: 'Compare',    icon: '◇' },
  { to: '/reports',    label: 'Reports',    icon: '▣' },
];

function Navbar() {
  const navigate = useNavigate();
  const user = localStorage.getItem('spendwise_user') || 'User';

  const handleLogout = () => {
    localStorage.removeItem('spendwise_auth');
    localStorage.removeItem('spendwise_user');
    navigate('/login');
  };

  return (
    <nav className="sticky top-0 z-40 backdrop-blur-xl bg-white/70 border-b border-gray-200/50 shadow-sm">
      <div className="mx-auto max-w-7xl flex items-center justify-between px-6 py-3">
        {/* Logo */}
        <NavLink to="/" className="flex items-center gap-2.5 group">
          <div className="relative">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-violet-500 via-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-violet-200/50 transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
              <span className="text-white font-bold text-sm">₹</span>
            </div>
            <div className="absolute -top-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-emerald-400 border-2 border-white animate-pulse"></div>
          </div>
          <span className="text-base font-extrabold bg-gradient-to-r from-violet-700 to-indigo-700 bg-clip-text text-transparent">
            SpendWise
          </span>
        </NavLink>

        {/* Nav links */}
        <div className="flex items-center gap-0.5 rounded-2xl bg-gray-100/80 p-1">
          {navItems.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `relative rounded-xl px-4 py-2 text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-white text-violet-700 shadow-sm'
                    : 'text-gray-500 hover:text-gray-800'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </div>

        {/* User + Logout */}
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 rounded-full bg-gradient-to-br from-violet-400 to-indigo-500 flex items-center justify-center ring-2 ring-violet-100">
            <span className="text-white text-xs font-bold">{user.charAt(0).toUpperCase()}</span>
          </div>
          <button
            onClick={handleLogout}
            className="text-xs font-medium text-gray-400 hover:text-red-500 transition-colors"
          >
            Sign out
          </button>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;

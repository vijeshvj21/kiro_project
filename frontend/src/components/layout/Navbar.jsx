import { NavLink } from 'react-router-dom';

const navItems = [
  { to: '/',           label: 'Dashboard',  icon: '📊' },
  { to: '/expenses',   label: 'Expenses',   icon: '💳' },
  { to: '/categories', label: 'Categories', icon: '🏷️' },
  { to: '/comparison', label: 'Comparison', icon: '📈' },
  { to: '/reports',    label: 'Reports',    icon: '📋' },
];

/**
 * Modern navigation bar with gradient background and icons.
 */
function Navbar() {
  return (
    <nav className="sticky top-0 z-40 border-b border-white/10 bg-gradient-to-r from-indigo-700 via-blue-700 to-purple-700 text-white shadow-xl shadow-blue-900/20 backdrop-blur-lg" aria-label="Main navigation">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        {/* Logo */}
        <NavLink to="/" className="flex items-center gap-3 group">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm ring-1 ring-white/20 transition-transform group-hover:scale-110">
            <svg viewBox="0 0 24 24" fill="none" className="h-6 w-6" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" fill="rgba(255,255,255,0.2)"/>
              <path d="M12 6v6l4 2" stroke="white" strokeWidth="2" strokeLinecap="round"/>
              <path d="M7 14h2l1-3h4l1 3h2" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              <circle cx="12" cy="12" r="1" fill="white"/>
            </svg>
          </div>
          <div>
            <span className="text-lg font-bold tracking-tight">SpendWise</span>
            <span className="ml-1.5 text-[10px] font-medium uppercase tracking-widest text-blue-200">Pro</span>
          </div>
        </NavLink>

        {/* Nav Links */}
        <ul className="flex gap-1" role="list">
          {navItems.map(({ to, label, icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-white/20 text-white shadow-inner ring-1 ring-white/25'
                      : 'text-blue-100 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                <span className="text-base">{icon}</span>
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}

export default Navbar;

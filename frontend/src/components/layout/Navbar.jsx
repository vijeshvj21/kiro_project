import { NavLink } from 'react-router-dom';

const navItems = [
  { to: '/',           label: 'Dashboard'   },
  { to: '/expenses',   label: 'Expenses'    },
  { to: '/categories', label: 'Categories'  },
  { to: '/comparison', label: 'Comparison'  },
  { to: '/reports',    label: 'Reports'     },
];

/**
 * Horizontal navigation bar with links to all main routes.
 */
function Navbar() {
  return (
    <nav className="bg-blue-700 text-white shadow-md" aria-label="Main navigation">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <span className="text-lg font-semibold tracking-wide">💰 Expense Tracker</span>

        <ul className="flex gap-1" role="list">
          {navItems.map(({ to, label }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === '/'}
                className={({ isActive }) =>
                  `rounded px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-white text-blue-700'
                      : 'text-blue-100 hover:bg-blue-600 hover:text-white'
                  }`
                }
              >
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

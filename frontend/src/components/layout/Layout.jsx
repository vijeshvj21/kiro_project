import Navbar from './Navbar';

/**
 * Root layout: Navbar at the top, then page content below.
 *
 * @param {object}       props
 * @param {React.ReactNode} props.children
 */
function Layout({ children }) {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="mx-auto max-w-6xl px-4 py-8">{children}</main>
    </div>
  );
}

export default Layout;

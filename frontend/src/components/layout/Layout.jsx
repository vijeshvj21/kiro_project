import Navbar from './Navbar';

/**
 * Root layout: Modern sticky navbar at the top, content below with gradient background.
 */
function Layout({ children }) {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-7xl px-6 py-8">{children}</main>
      
      {/* Footer */}
      <footer className="border-t border-gray-200 bg-white/50 backdrop-blur-sm mt-12">
        <div className="mx-auto max-w-7xl px-6 py-4 text-center">
          <p className="text-xs text-gray-400">
            SpendWise Pro — Smart expense tracking powered by Gmail
          </p>
        </div>
      </footer>
    </div>
  );
}

export default Layout;

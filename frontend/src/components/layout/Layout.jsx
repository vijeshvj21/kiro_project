import Navbar from './Navbar';

function Layout({ children }) {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-7xl px-6 py-8 animate-fade-in-up">
        {children}
      </main>
    </div>
  );
}

export default Layout;

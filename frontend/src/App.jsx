import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/DashboardPage';
import ExpenseListPage from './pages/ExpenseListPage';
import ExpenseFormPage from './pages/ExpenseFormPage';
import CategoryPage from './pages/CategoryPage';
import ComparisonPage from './pages/ComparisonPage';
import ReportPage from './pages/ReportPage';
import NotFoundPage from './pages/NotFoundPage';
import LoginPage from './pages/LoginPage';

/**
 * Simple auth guard — checks localStorage for auth token.
 * Replace with real auth in production.
 */
function ProtectedRoute({ children }) {
  const isAuth = localStorage.getItem('spendwise_auth') === 'true';
  if (!isAuth) return <Navigate to="/login" replace />;
  return children;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public route — Login page (no Layout) */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected routes — wrapped in Layout */}
        <Route path="/" element={
          <ProtectedRoute>
            <Layout><DashboardPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/expenses" element={
          <ProtectedRoute>
            <Layout><ExpenseListPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/expenses/new" element={
          <ProtectedRoute>
            <Layout><ExpenseFormPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/expenses/:id/edit" element={
          <ProtectedRoute>
            <Layout><ExpenseFormPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/categories" element={
          <ProtectedRoute>
            <Layout><CategoryPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/comparison" element={
          <ProtectedRoute>
            <Layout><ComparisonPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="/reports" element={
          <ProtectedRoute>
            <Layout><ReportPage /></Layout>
          </ProtectedRoute>
        } />
        <Route path="*" element={<Layout><NotFoundPage /></Layout>} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

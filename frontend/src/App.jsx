import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/DashboardPage';
import ExpenseListPage from './pages/ExpenseListPage';
import ExpenseFormPage from './pages/ExpenseFormPage';
import CategoryPage from './pages/CategoryPage';
import ComparisonPage from './pages/ComparisonPage';
import ReportPage from './pages/ReportPage';

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/"                   element={<DashboardPage />} />
          <Route path="/expenses"           element={<ExpenseListPage />} />
          <Route path="/expenses/new"       element={<ExpenseFormPage />} />
          <Route path="/expenses/:id/edit"  element={<ExpenseFormPage />} />
          <Route path="/categories"         element={<CategoryPage />} />
          <Route path="/comparison"         element={<ComparisonPage />} />
          <Route path="/reports"            element={<ReportPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

export default App;

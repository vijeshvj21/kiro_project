import { useState } from 'react';
import ReportForm from '../components/report/ReportForm';
import ReportDownloadButton from '../components/report/ReportDownloadButton';

/**
 * Page that lets the user configure and download an expense report.
 */
function ReportPage() {
  const [reportParams, setReportParams] = useState(null);

  return (
    <div className="mx-auto max-w-lg space-y-6 py-8">
      <h1 className="text-2xl font-bold text-gray-900">Reports</h1>

      <ReportForm onGenerate={setReportParams} />

      {reportParams && (
        <ReportDownloadButton reportParams={reportParams} />
      )}
    </div>
  );
}

export default ReportPage;

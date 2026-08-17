import { useState } from 'react';
import ReportForm from '../components/report/ReportForm';
import ReportDownloadButton from '../components/report/ReportDownloadButton';

/**
 * Page that lets the user configure and download an expense report.
 */
function ReportPage() {
  const [reportParams, setReportParams] = useState(null);

  return (
    <div className="space-y-6">
      <div className="mb-2">
        <h1 className="text-3xl font-extrabold bg-gradient-to-r from-gray-900 via-violet-900 to-indigo-900 bg-clip-text text-transparent">Reports</h1>
        <p className="text-sm text-gray-500 mt-1">Generate and download your expense reports.</p>
      </div>

      <div className="max-w-lg space-y-6">
        <ReportForm onGenerate={setReportParams} />
        {reportParams && <ReportDownloadButton reportParams={reportParams} />}
      </div>
    </div>
  );
}

export default ReportPage;

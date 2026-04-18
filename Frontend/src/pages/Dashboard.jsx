import { useEffect, useState } from 'react';
import DocumentUpload from '../components/DocumentUpload';
import ChatInterface from '../components/ChatInterface';
import Timeline from '../components/Timeline';
import FormularySearch from '../components/FormularySearch';
import { clearDocuments } from '../services/api';

function Dashboard() {
  const [uploadCount, setUploadCount] = useState(0);
  const [activeTab, setActiveTab] = useState('upload'); // 'upload' or 'timeline'

  useEffect(() => {
    // Session is now persistent. Manual clear button is allowed instead of auto-clear.
    console.log("Dashboard loaded - persistent user session active");
  }, []);

  const handleUploadSuccess = () => {
    setUploadCount(prev => prev + 1);
  };

  return (
    <div className="flex-1 w-full h-full min-h-0 flex flex-col overflow-hidden bg-[radial-gradient(circle_at_top_left,_#1e293b,_#0f172a)]">
      {/* Top action bar */}
      <div className="px-6 pt-4 max-w-[1600px] mx-auto w-full">
        <div className="max-w-[380px]">
          <FormularySearch />
        </div>
      </div>

      <main className="mx-auto flex flex-1 w-full h-full min-h-0 max-w-[1600px] gap-6 overflow-hidden p-6 pt-2">
        <div className="flex flex-col shrink-0 w-[380px] h-full min-h-0 overflow-hidden rounded-[20px] border border-white/10 bg-slate-800/50 p-6 shadow-[0_8px_32px_rgba(0,0,0,0.3)] backdrop-blur-[12px]">
          
          {/* Tab Toggle */}
          <div className="flex p-1 mb-6 rounded-xl bg-slate-900/50 border border-white/5">
            <button 
              onClick={() => setActiveTab('upload')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${activeTab === 'upload' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
            >
              UPLOAD
            </button>
            <button 
              onClick={() => setActiveTab('timeline')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${activeTab === 'timeline' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}
            >
              HISTORY
            </button>
          </div>

          <div className="flex-1 overflow-hidden">
            {activeTab === 'upload' ? (
              <DocumentUpload onUploadSuccess={handleUploadSuccess} />
            ) : (
              <Timeline key={uploadCount} />
            )}
          </div>
        </div>

        <div className="flex flex-col flex-1 h-full min-w-0 min-h-0 overflow-hidden rounded-[20px] border border-white/10 bg-slate-800/50 p-px shadow-[0_8px_32px_rgba(0,0,0,0.3)] backdrop-blur-[12px]">
          <ChatInterface key={uploadCount} />
        </div>
      </main>
    </div>
  );
}

export default Dashboard;

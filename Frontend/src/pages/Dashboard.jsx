import { useEffect, useState } from 'react';
import DocumentUpload from '../components/DocumentUpload';
import ChatInterface from '../components/ChatInterface';
import { clearDocuments } from '../services/api';


function Dashboard() {
  const [uploadCount, setUploadCount] = useState(0);

  // Requirement: Forget all documents on reload
  useEffect(() => {
    const initSession = async () => {
      try {
        await clearDocuments();
        console.log("Documents cleared for new session");
      } catch (err) {
        console.error("Failed to clear documents on session start", err);
      }
    };
    initSession();
  }, []);

  const handleUploadSuccess = (response) => {
    setUploadCount(prev => prev + 1);
  };

  return (
    <div className="flex-1 w-full h-full min-h-0 flex flex-col overflow-hidden bg-[radial-gradient(circle_at_top_left,_#1e293b,_#0f172a)]">
      <main className="mx-auto flex flex-1 w-full h-full min-h-0 max-w-[1600px] gap-6 overflow-hidden p-6">
        <div className="flex shrink-0 w-[380px] h-full min-h-0 overflow-hidden rounded-[20px] border border-white/10 bg-slate-800/50 p-px shadow-[0_8px_32px_rgba(0,0,0,0.3)] backdrop-blur-[12px]">
          <DocumentUpload onUploadSuccess={handleUploadSuccess} />
        </div>

        <div className="flex flex-col flex-1 h-full min-w-0 min-h-0 overflow-hidden rounded-[20px] border border-white/10 bg-slate-800/50 p-px shadow-[0_8px_32px_rgba(0,0,0,0.3)] backdrop-blur-[12px]">
          <ChatInterface key={uploadCount} />
        </div>
      </main>
    </div>
  );
}



export default Dashboard;

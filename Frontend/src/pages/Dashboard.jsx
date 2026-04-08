import { useEffect, useState } from 'react';
import DocumentUpload from '../components/DocumentUpload';
import ChatInterface from '../components/ChatInterface';
import { clearDocuments } from '../services/api';
import '../App.css';

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
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1>🏥 MediSure AI</h1>
          <p className="tagline">Intelligent Document Analysis</p>
        </div>
        <div className="header-actions">
          <button
            className="logout-btn"
            onClick={() => {
              localStorage.removeItem("user");
              window.location.href = "/login";
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <main className="app-main">
        <div className="left-panel">
          <DocumentUpload onUploadSuccess={handleUploadSuccess} />
        </div>
        
        <div className="right-panel">
          <ChatInterface key={uploadCount} />
        </div>
      </main>

      <footer className="app-footer">
        <p>Built with Spring AI, Ollama, and React</p>
      </footer>
    </div>
  );
}



export default Dashboard;
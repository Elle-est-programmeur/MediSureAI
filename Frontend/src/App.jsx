import { useState } from 'react';
import DocumentUpload from './components/DocumentUpload';
import ChatInterface from './components/ChatInterface';
import './App.css';

function App() {
  const [uploadCount, setUploadCount] = useState(0);

  const handleUploadSuccess = (response) => {
    setUploadCount(prev => prev + 1);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>🏥 MediSure AI</h1>
        <p className="tagline">RAG-Powered Document Intelligence</p>
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

export default App;

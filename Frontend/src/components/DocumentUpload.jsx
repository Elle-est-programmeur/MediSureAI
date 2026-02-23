import { useState } from 'react';
import { uploadDocument } from '../services/api';
import './DocumentUpload.css';

function DocumentUpload({ onUploadSuccess }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      setFile(selectedFile);
      setMessage('');
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file first');
      return;
    }

    setUploading(true);
    setMessage('');
    setError('');

    try {
      const response = await uploadDocument(file);
      
      if (response.status === 'COMPLETED') {
        setMessage(`✓ ${response.fileName} uploaded successfully! Created ${response.chunkCount} chunks.`);
        setFile(null);
        // Reset file input
        document.getElementById('file-input').value = '';
        
        if (onUploadSuccess) {
          onUploadSuccess(response);
        }
      } else {
        setError(`Failed to process document: ${response.message}`);
      }
    } catch (err) {
      setError(`Error uploading file: ${err.message}`);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="document-upload">
      <h2>📄 Upload Document</h2>
      <p className="upload-description">
        Upload PDF or text documents to add them to the knowledge base
      </p>
      
      <div className="upload-container">
        <input
          id="file-input"
          type="file"
          accept=".pdf,.txt,.md"
          onChange={handleFileChange}
          disabled={uploading}
          className="file-input"
        />
        
        {file && (
          <div className="file-info">
            <span className="file-name">{file.name}</span>
            <span className="file-size">
              ({(file.size / 1024).toFixed(2)} KB)
            </span>
          </div>
        )}
        
        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          className="upload-button"
        >
          {uploading ? '⏳ Processing...' : '⬆️ Upload'}
        </button>
      </div>

      {message && <div className="success-message">{message}</div>}
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}

export default DocumentUpload;

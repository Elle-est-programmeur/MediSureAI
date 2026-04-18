import { useState } from 'react';
import { uploadDocuments } from '../services/api';
import './DocumentUpload.css';

function DocumentUpload({ onUploadSuccess }) {
  const [files, setFiles] = useState([]);
  const [documentType, setDocumentType] = useState('MEDICAL_REPORT');
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setFiles(Array.from(e.target.files));
      setMessage('');
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!files || files.length === 0) {
      setError('Please select at least one file first');
      return;
    }

    setUploading(true);
    setMessage('');
    setError('');

    try {
      const responseBatch = await uploadDocuments(files, documentType);
      
      const successCount = responseBatch.filter(r => r.status === 'UPLOADED' || r.status === 'COMPLETED').length;
      if (successCount > 0) {
        setMessage(`✓ ${successCount} file(s) uploaded successfully! Background processing and vectorization started.`);
        setFiles([]);
        document.getElementById('file-input').value = '';
        
        if (onUploadSuccess) {
          onUploadSuccess(responseBatch);
        }
      } else {
        setError(`Failed to initiate document processing.`);
      }
    } catch (err) {
      setError(`Error uploading files: ${err.message}`);
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
        <div className="type-selector mb-4">
          <label className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1.5 block">Document Type</label>
          <select 
            value={documentType}
            onChange={(e) => setDocumentType(e.target.value)}
            className="w-full bg-slate-900 border border-white/10 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
          >
            <option value="MEDICAL_REPORT">Medical Report / Lab Result</option>
            <option value="INSURANCE_POLICY">Insurance Policy / Benefits</option>
            <option value="PRESCRIPTION">Prescription</option>
            <option value="OTHER">Other / General</option>
          </select>
        </div>

        <input
          id="file-input"
          type="file"
          accept=".pdf,.txt,.md,.docx"
          multiple
          onChange={handleFileChange}
          disabled={uploading}
          className="file-input"
        />
        
        {files.length > 0 && (
          <div className="file-info">
            <span className="file-name">{files.length} file(s) ready</span>
            <span className="file-size">
              ({(files.reduce((acc, f) => acc + f.size, 0) / 1024).toFixed(2)} KB total)
            </span>
          </div>
        )}
        
        <button
          onClick={handleUpload}
          disabled={files.length === 0 || uploading}
          className="upload-button"
        >
          {uploading ? '⏳ Vectorizing...' : '⬆️ Upload'}
        </button>
      </div>

      {message && <div className="success-message">{message}</div>}
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}

export default DocumentUpload;

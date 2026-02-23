import { useState } from 'react';
import { askQuestion } from '../services/api';
import './ChatInterface.css';

function ChatInterface() {
  const [question, setQuestion] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!question.trim()) {
      return;
    }

    const userMessage = {
      type: 'question',
      content: question,
      timestamp: new Date().toISOString()
    };

    setChatHistory(prev => [...prev, userMessage]);
    setQuestion('');
    setLoading(true);

    try {
      const response = await askQuestion(question);
      
      const aiMessage = {
        type: 'answer',
        content: response.answer,
        relevantChunks: response.relevantChunks,
        documentsUsed: response.documentsUsed,
        timestamp: new Date().toISOString()
      };

      setChatHistory(prev => [...prev, aiMessage]);
    } catch (error) {
      const errorMessage = {
        type: 'error',
        content: `Error: ${error.message}`,
        timestamp: new Date().toISOString()
      };
      setChatHistory(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chat-interface">
      <h2>💬 Ask Questions</h2>
      <p className="chat-description">
        Ask questions about your uploaded documents
      </p>

      <div className="chat-container">
        <div className="chat-history">
          {chatHistory.length === 0 ? (
            <div className="empty-state">
              <p>👋 Upload some documents and start asking questions!</p>
            </div>
          ) : (
            chatHistory.map((message, index) => (
              <div key={index} className={`message ${message.type}`}>
                {message.type === 'question' && (
                  <div className="question-message">
                    <div className="message-label">You</div>
                    <div className="message-content">{message.content}</div>
                  </div>
                )}
                
                {message.type === 'answer' && (
                  <div className="answer-message">
                    <div className="message-label">AI Assistant</div>
                    <div className="message-content">{message.content}</div>
                    {message.documentsUsed > 0 && (
                      <div className="message-meta">
                        📚 Used {message.documentsUsed} document{message.documentsUsed > 1 ? 's' : ''}
                      </div>
                    )}
                  </div>
                )}
                
                {message.type === 'error' && (
                  <div className="error-message">
                    <div className="message-content">{message.content}</div>
                  </div>
                )}
              </div>
            ))
          )}
          
          {loading && (
            <div className="message answer">
              <div className="answer-message loading">
                <div className="message-label">AI Assistant</div>
                <div className="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="chat-input-form">
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask a question about your documents..."
            disabled={loading}
            className="chat-input"
          />
          <button
            type="submit"
            disabled={!question.trim() || loading}
            className="send-button"
          >
            {loading ? '⏳' : '➤'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default ChatInterface;

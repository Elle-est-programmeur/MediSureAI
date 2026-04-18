import { useEffect, useRef, useState } from 'react';
import { askQuestion } from '../services/api';

function ChatInterface() {
  const [question, setQuestion] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const chatHistoryRef = useRef(null);

  useEffect(() => {
    if (!chatHistoryRef.current) return;
    chatHistoryRef.current.scrollTop = chatHistoryRef.current.scrollHeight;
  }, [chatHistory, loading]);


  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    
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
        content: response.finalAnswer || response.answer || "Sorry, I couldn't generate a response.",
        confidenceScore: response.confidenceScore ? response.confidenceScore.toFixed(2) : null,
        detectedIntent: response.detectedIntent || null,
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
    <div className="flex flex-col h-full w-full overflow-hidden bg-transparent p-6">
      {/* Header - Fixed at Top */}
      <div className="shrink-0 mb-6">
        <h2 className="text-2xl font-bold text-white mb-2 tracking-tight">💬 Ask Questions</h2>
        <p className="text-sm text-slate-300">
          Ask questions or run specialized <span className="text-blue-400 font-semibold italic">Coverage Gap Analysis</span>
        </p>
      </div>

      {/* Main Container - Flex-1 and hidden overflow */}
      <div className="flex flex-col flex-1 min-h-0 overflow-hidden bg-slate-900/50 rounded-2xl border border-white/10 shadow-inner">
        
        {/* Chat Messages - Scrollable Section */}
        <div 
          className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6" 
          ref={chatHistoryRef}
        >
          {chatHistory.length === 0 ? (
            <div className="flex items-center justify-center h-full text-slate-400 text-sm">
              <div className="flex flex-col items-center gap-2">
                <span className="text-3xl">👋</span>
                <p>Upload some documents and start asking questions!</p>
                <p className="text-xs text-slate-500 mt-2">Try mentioning a procedure and clicking 'Analyze'</p>
              </div>
            </div>
          ) : (
            chatHistory.map((message, index) => (
              <div key={index} className="flex w-full animate-in fade-in slide-in-from-bottom-2 duration-300">
                {message.type === 'question' && (
                  <div className="ml-auto flex flex-col items-end max-w-[80%]">
                    <span className="text-xs font-semibold text-white/70 mb-1.5 px-1">You</span>
                    <div className="bg-gradient-to-br from-blue-500 to-indigo-600 text-white px-4 py-3 rounded-2xl rounded-tr-sm shadow-md text-sm leading-relaxed whitespace-pre-wrap break-words">
                      {message.content}
                    </div>
                  </div>
                )}
                
                {message.type === 'answer' && (
                  <div className="mr-auto flex flex-col items-start max-w-[80%]">
                    <span className="text-xs font-semibold text-white/70 mb-1.5 px-1">AI Assistant</span>
                    <div className="bg-slate-800 text-slate-100 px-4 py-3 border border-white/5 rounded-2xl rounded-tl-sm shadow-md text-sm leading-relaxed whitespace-pre-wrap break-words">
                      {message.content}
                      {(message.confidenceScore || message.detectedIntent) && (
                        <div className="mt-3 pt-3 border-t border-slate-700/50 text-xs text-slate-400 flex flex-wrap gap-2 items-center">
                          {message.detectedIntent && (
                            <span className="bg-indigo-500/10 text-indigo-400 px-2.5 py-1 rounded-md border border-indigo-500/20 font-medium">
                              🔍 Analyzer: {message.detectedIntent.replace(/_/g, ' ')}
                            </span>
                          )}
                          {message.confidenceScore && (
                            <span className={`px-2.5 py-1 rounded-md border font-medium ${
                              parseFloat(message.confidenceScore) > 7.5 ? 'bg-green-500/10 text-green-400 border-green-500/20' : 
                              parseFloat(message.confidenceScore) > 5.0 ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20' : 
                              'bg-red-500/10 text-red-400 border-red-500/20'
                            }`}>
                              🛡️ Confidence: {message.confidenceScore}/10
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                )}
                
                {message.type === 'error' && (
                  <div className="mx-auto w-full max-w-[90%] mt-2">
                    <div className="bg-red-500/10 border border-red-500/20 text-red-400 px-4 py-3 rounded-xl text-sm text-center">
                      {message.content}
                    </div>
                  </div>
                )}
              </div>
            ))
          )}
          
          {loading && (
            <div className="mr-auto flex flex-col items-start max-w-[80%]">
              <span className="text-xs font-semibold text-white/70 mb-1.5 px-1 font-mono uppercase tracking-widest animate-pulse">
                SRLM Engine Processing
              </span>
              <div className="bg-slate-800 border border-blue-500/20 px-5 py-4 rounded-2xl rounded-tl-sm shadow-[0_0_15px_rgba(59,130,246,0.1)] flex flex-col gap-3">
                <div className="flex gap-1.5 items-center">
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '0ms' }} />
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '150ms' }} />
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '300ms' }} />
                </div>
                <div className="flex flex-col gap-1">
                  <div className="h-1.5 w-32 bg-slate-700/50 rounded-full overflow-hidden">
                    <div className="h-full bg-blue-500/50 animate-[shimmer_2s_infinite]" style={{ width: '40%' }}></div>
                  </div>
                  <span className="text-[10px] text-slate-500 font-medium">Synthesizing multiple reasoning paths...</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Input Box - Fixed at Bottom */}
        <form 
          onSubmit={handleSubmit} 
          className="shrink-0 p-4 bg-slate-800/80 border-t border-white/10 backdrop-blur-md flex flex-col gap-3"
        >
          <div className="flex gap-3">
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask a question or name a procedure..."
            disabled={loading}
            className="flex-1 bg-slate-900 border border-slate-700 text-white placeholder-slate-400 px-4 py-3 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed min-w-0"
          />
          <button
            type="submit"
            disabled={!question.trim() || loading}
            className="shrink-0 px-5 bg-gradient-to-br from-blue-500 to-indigo-600 hover:from-blue-400 hover:to-indigo-500 disabled:from-slate-700 disabled:to-slate-700 disabled:text-slate-400 text-white rounded-xl font-medium shadow-md transition-all disabled:opacity-50 disabled:cursor-not-allowed hover:-translate-y-0.5 active:translate-y-0 flex items-center justify-center transform"
          >
            {loading ? (
              <span className="animate-spin w-5 h-5 border-2 border-white/30 border-t-white rounded-full" />
            ) : (
              '➤'
            )}
          </button>
          </div>
          
        </form>
      </div>
    </div>
  );
}

export default ChatInterface;

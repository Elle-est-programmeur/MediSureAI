# MediSure AI - RAG Architecture

## System Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                          │
│                    (React + Vite Frontend)                      │
│  ┌──────────────────────┐     ┌─────────────────────────────┐ │
│  │  DocumentUpload      │     │    ChatInterface            │ │
│  │  - File selection    │     │    - Question input         │ │
│  │  - Upload progress   │     │    - Chat history           │ │
│  │  - Status feedback   │     │    - Answer display         │ │
│  └──────────────────────┘     └─────────────────────────────┘ │
└────────────────────┬───────────────────────┬───────────────────┘
                     │                       │
                     │ HTTP/REST API         │
                     │ (Axios)               │
                     ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND API LAYER                            │
│                   (Spring Boot + Spring AI)                     │
│  ┌──────────────────────┐     ┌─────────────────────────────┐ │
│  │ DocumentController   │     │    ChatController           │ │
│  │ POST /upload         │     │    POST /ask                │ │
│  │ GET /documents       │     │                             │ │
│  └──────────┬───────────┘     └──────────┬──────────────────┘ │
│             │                             │                    │
│             ▼                             ▼                    │
│  ┌──────────────────────┐     ┌─────────────────────────────┐ │
│  │  DocumentService     │     │    RAGService               │ │
│  │  - Text extraction   │     │    - Similarity search      │ │
│  │  - Chunking          │     │    - Context assembly       │ │
│  │  - Embedding gen     │     │    - LLM prompting          │ │
│  │  - Vector storage    │     │    - Answer generation      │ │
│  └──────────┬───────────┘     └──────────┬──────────────────┘ │
└─────────────┼────────────────────────────┼────────────────────┘
              │                            │
              │                            │
    ┌─────────▼─────────┐        ┌────────▼────────────┐
    │                   │        │                     │
    │  VectorStore      │        │  Ollama LLM         │
    │  (PgVector)       │◄───────│  - Chat Model       │
    │                   │        │  - Embedding Model  │
    │  - Embeddings     │        │                     │
    │  - Similarity     │        │  llama3.2:3b        │
    │    Search (HNSW)  │        │  nomic-embed-text   │
    │                   │        │                     │
    └───────────────────┘        └─────────────────────┘
              │                            │
              │                            │
    ┌─────────▼─────────┐        ┌────────▼────────────┐
    │                   │        │                     │
    │  PostgreSQL       │        │  Ollama Service     │
    │  with pgvector    │        │  (Docker)           │
    │                   │        │  Port: 11434        │
    │  Port: 5432       │        │                     │
    │                   │        │                     │
    └───────────────────┘        └─────────────────────┘
```

## Document Upload Flow

```
1. User selects file
       ↓
2. Frontend uploads via FormData
       ↓
3. DocumentController receives file
       ↓
4. DocumentService processes:
       ├─► Extract text (PDF/TXT)
       ├─► Split into chunks (TokenTextSplitter)
       │   └─► 512 tokens/chunk, 128 overlap
       ├─► Generate embeddings (nomic-embed-text)
       │   └─► 768-dimensional vectors
       └─► Store in pgvector
           └─► With metadata (filename, doc_id, timestamp)
       ↓
5. Document entity saved to DB
       ├─► Status: COMPLETED
       ├─► Chunk count
       └─► Upload timestamp
       ↓
6. Success response to frontend
```

## Question Answering Flow

```
1. User enters question
       ↓
2. Frontend sends to /api/chat/ask
       ↓
3. ChatController receives request
       ↓
4. RAGService processes:
       ├─► Convert question to embedding
       ├─► Similarity search in pgvector
       │   └─► Find top-K most similar chunks
       ├─► Extract relevant context
       ├─► Build prompt:
       │   ┌──────────────────────────────┐
       │   │ Context: [relevant chunks]   │
       │   │ Question: [user question]    │
       │   │ Answer:                      │
       │   └──────────────────────────────┘
       └─► Send to Ollama LLM
           └─► llama3.2:3b generates answer
       ↓
5. Response assembled:
       ├─► Answer text
       ├─► Relevant chunks preview
       └─► Document count used
       ↓
6. Frontend displays in chat
```

## Data Model

### Document Entity
```
┌──────────────────────────┐
│       Document           │
├──────────────────────────┤
│ id: Long                 │
│ fileName: String         │
│ contentType: String      │
│ fileSize: Long           │
│ chunkCount: Integer      │
│ uploadedAt: DateTime     │
│ status: String           │
│ errorMessage: String     │
└──────────────────────────┘
```

### Vector Store Entry
```
┌──────────────────────────┐
│    VectorStore Entry     │
├──────────────────────────┤
│ id: UUID                 │
│ content: String          │
│ embedding: float[768]    │
│ metadata: JSON           │
│   ├─ document_id         │
│   ├─ file_name           │
│   └─ upload_time         │
└──────────────────────────┘
```

## Technology Stack Details

### Backend Stack
- **Framework**: Spring Boot 4.0.3
- **AI Integration**: Spring AI 2.0.0-M2
- **Java Version**: 21
- **Build Tool**: Maven
- **Dependencies**:
  - spring-boot-starter-data-jpa
  - spring-boot-starter-web
  - spring-ai-ollama-spring-boot-starter
  - spring-ai-starter-vector-store-pgvector
  - spring-ai-pdf-document-reader
  - apache-tika (2.9.1)
  - postgresql driver
  - lombok

### Frontend Stack
- **Framework**: React 19
- **Build Tool**: Vite 7
- **HTTP Client**: Axios 1.6.7
- **Styling**: Pure CSS (no framework)

### Infrastructure
- **Database**: PostgreSQL 16 with pgvector extension
- **LLM Platform**: Ollama
  - Chat: llama3.2:3b (3 billion parameters)
  - Embeddings: nomic-embed-text (768 dimensions)
- **Containerization**: Docker Compose

## Vector Search Configuration

### PgVector Settings
```
Dimensions: 768 (matches nomic-embed-text)
Distance Type: COSINE_DISTANCE
Index Type: HNSW (Hierarchical Navigable Small World)
  - Fast approximate nearest neighbor search
  - Better performance for large datasets
```

### Chunking Strategy
```
Token-based splitting:
- Chunk Size: 512 tokens
- Overlap: 128 tokens (25%)
- Min Chunk: 5 tokens
- Max Chunk: 5000 tokens
- Preserve Sentences: true
```

### Retrieval Strategy
```
Top-K Search: 5 chunks (default)
Distance Metric: Cosine similarity
Threshold: None (always returns top-K)
```

## API Contract

### Upload Document
```http
POST /api/documents/upload
Content-Type: multipart/form-data

Request:
  file: <binary>

Response:
{
  "documentId": 123,
  "fileName": "document.pdf",
  "status": "COMPLETED",
  "chunkCount": 42,
  "message": "Document processed successfully"
}
```

### Ask Question
```http
POST /api/chat/ask
Content-Type: application/json

Request:
{
  "question": "What is this about?",
  "topK": 5
}

Response:
{
  "question": "What is this about?",
  "answer": "Based on the documents...",
  "relevantChunks": ["chunk1...", "chunk2..."],
  "documentsUsed": 2
}
```

### Get Documents
```http
GET /api/documents
GET /api/documents/completed

Response:
[
  {
    "id": 1,
    "fileName": "doc1.pdf",
    "contentType": "application/pdf",
    "fileSize": 1024000,
    "chunkCount": 25,
    "uploadedAt": "2024-02-23T10:30:00",
    "status": "COMPLETED"
  }
]
```

## Performance Considerations

### Embedding Generation
- Time: ~100-500ms per chunk
- Batch processing: Sequential
- Model: nomic-embed-text (lightweight, fast)

### Vector Search
- HNSW index: O(log n) approximate search
- Typical query time: 10-100ms for 1M vectors
- Scales well with dataset size

### LLM Response
- Model: llama3.2:3b (fast, small)
- Typical response time: 1-5 seconds
- Context window: ~8K tokens
- Generation speed: ~20-50 tokens/sec

## Security Features

### Current Implementation
- CORS enabled for localhost:5173, localhost:3000
- CSRF disabled (development mode)
- All endpoints publicly accessible
- File type validation (PDF, TXT only)
- File size limit (50MB)

### Production Recommendations
- Add JWT authentication
- Implement rate limiting
- Enable CSRF protection
- Add API key validation
- Restrict CORS to production domain
- Add request validation
- Implement file scanning (virus check)

## Scalability Considerations

### Current Limitations
- Single backend instance
- Synchronous document processing
- No caching layer
- Direct database queries

### Scaling Options
1. **Horizontal Scaling**
   - Load balance multiple backend instances
   - Share PostgreSQL database
   - Stateless architecture (ready for this)

2. **Async Processing**
   - Queue document processing (RabbitMQ/Kafka)
   - Background workers for embedding generation
   - Webhook notifications on completion

3. **Caching**
   - Redis for frequent queries
   - Cache embeddings
   - Cache LLM responses for identical questions

4. **Database Optimization**
   - Connection pooling (already configured)
   - Read replicas for queries
   - Partitioning for large datasets

## Monitoring Points

### Key Metrics to Track
1. Document processing time
2. Embedding generation time
3. Vector search latency
4. LLM response time
5. Success/failure rates
6. Database query performance
7. Memory usage
8. Disk space (documents & vectors)

### Health Checks
- Database connectivity
- Ollama availability
- Vector store status
- Document processing queue

## Future Enhancements

### Short Term
- [ ] Document deletion
- [ ] Multiple file formats (DOCX, CSV)
- [ ] Conversation history persistence
- [ ] User feedback on answers
- [ ] Document preview/viewer

### Medium Term
- [ ] Multi-user support with authentication
- [ ] Document collections/folders
- [ ] Advanced search filters
- [ ] Streaming responses (SSE)
- [ ] Mobile responsive design

### Long Term
- [ ] Multi-language support
- [ ] Custom fine-tuned models
- [ ] Advanced analytics dashboard
- [ ] Integration with external sources
- [ ] GraphQL API
- [ ] Kubernetes deployment

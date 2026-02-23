# MediSure AI - RAG Application Setup Guide

## Overview
MediSure AI is a Retrieval-Augmented Generation (RAG) application that allows users to upload documents, automatically process them into embeddings, and ask questions about the content using an AI assistant powered by Ollama.

## Architecture
- **Backend**: Spring Boot with Spring AI
- **Vector Database**: PostgreSQL with pgvector extension
- **LLM**: Ollama (llama3.2:3b for chat, nomic-embed-text for embeddings)
- **Frontend**: React with Vite

## Prerequisites
Make sure you have the following installed:
- Java 21
- Maven
- Node.js (v18 or higher)
- Docker & Docker Compose
- Ollama

## Setup Instructions

### 1. Start Database and Ollama Services

Navigate to the Backend directory and start the services using Docker Compose:

```bash
cd Backend
docker-compose -f compose.yml up -d
```

This will start:
- PostgreSQL with pgvector on port 5432
- Ollama on port 11434

### 2. Pull Required Ollama Models

Pull the required models for the application:

```bash
# Pull the chat model (llama3.2:3b - faster model)
ollama pull llama3.2:3b

# Pull the embedding model
ollama pull nomic-embed-text
```

You can verify the models are installed:
```bash
ollama list
```

### 3. Start the Backend

From the Backend directory, run:

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

The backend will start on `http://localhost:8080`

### 4. Install Frontend Dependencies

Navigate to the Frontend directory and install dependencies:

```bash
cd Frontend
npm install
```

### 5. Start the Frontend

Run the development server:

```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## Usage

### Upload Documents
1. Open the application in your browser at `http://localhost:5173`
2. Click the file input or drag and drop a document (PDF or TXT)
3. Click "Upload" to process the document
4. The system will:
   - Extract text from the document
   - Split it into chunks (512 tokens each)
   - Generate embeddings using nomic-embed-text
   - Store embeddings in PostgreSQL pgvector

### Ask Questions
1. After uploading documents, type your question in the chat interface
2. The system will:
   - Convert your question to an embedding
   - Search for relevant document chunks using similarity search
   - Send the relevant context + question to Ollama
   - Display the AI-generated answer

## API Endpoints

### Document Management
- `POST /api/documents/upload` - Upload a document (PDF or TXT)
- `GET /api/documents` - Get all documents
- `GET /api/documents/completed` - Get successfully processed documents

### Chat
- `POST /api/chat/ask` - Ask a question about uploaded documents

### Request Examples

**Upload Document:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/path/to/document.pdf"
```

**Ask Question:**
```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this document about?", "topK": 5}'
```

## Configuration

### Backend Configuration (application.properties)
Key configurations:
- **Ollama URL**: `spring.ai.ollama.base-url=http://localhost:11434`
- **Chat Model**: `spring.ai.ollama.chat.options.model=llama3.2:3b`
- **Embedding Dimensions**: `spring.ai.vectorstore.pgvector.dimensions=768`
- **Max File Size**: `spring.servlet.multipart.max-file-size=50MB`

### Frontend Configuration
API base URL is configured in `src/services/api.js`:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

## Project Structure

### Backend
```
Backend/src/main/java/com/example/Backend/
├── config/
│   ├── SecurityConfig.java         # CORS and security configuration
│   ├── VectorStoreConfig.java      # Embedding model configuration
│   └── FileUploadConfig.java       # File upload settings
├── controller/
│   ├── DocumentController.java     # Document upload endpoints
│   └── ChatController.java         # Q&A endpoints
├── service/
│   ├── DocumentService.java        # Document processing & chunking
│   └── RAGService.java            # Question answering logic
├── model/
│   └── Document.java              # Document entity
├── dto/
│   ├── DocumentUploadResponse.java
│   ├── QuestionRequest.java
│   └── QuestionResponse.java
└── repository/
    └── DocumentRepository.java
```

### Frontend
```
Frontend/src/
├── components/
│   ├── DocumentUpload.jsx         # Document upload component
│   ├── DocumentUpload.css
│   ├── ChatInterface.jsx          # Q&A chat interface
│   └── ChatInterface.css
├── services/
│   └── api.js                     # API client
├── App.jsx                        # Main application
├── App.css
└── main.jsx
```

## Troubleshooting

### Backend won't start
- Ensure PostgreSQL is running: `docker ps`
- Check if port 8080 is available
- Verify Java 21 is installed: `java -version`

### Ollama connection errors
- Ensure Ollama is running: `docker ps`
- Check if models are downloaded: `ollama list`
- Verify Ollama is accessible: `curl http://localhost:11434`

### Frontend connection errors
- Ensure backend is running on port 8080
- Check browser console for CORS errors
- Verify API URL in `src/services/api.js`

### Document upload fails
- Check file size (max 50MB)
- Ensure file type is PDF or TXT
- Check backend logs for detailed errors

## Advanced Configuration

### Using a Different Ollama Model
Edit `Backend/src/main/resources/application.properties`:
```properties
# For faster responses, use smaller models
spring.ai.ollama.chat.options.model=llama3.2:1b

# For better quality, use larger models
spring.ai.ollama.chat.options.model=llama3.2:8b
```

### Adjusting Chunk Size
Edit `DocumentService.java`:
```java
TokenTextSplitter splitter = new TokenTextSplitter(
    512,  // chunk size
    128,  // overlap
    5,    // min chunk size
    5000, // max chunk size
    true  // preserve sentences
);
```

### Changing Number of Retrieved Documents
Edit the frontend request or pass different topK:
```javascript
const response = await askQuestion(question, 10); // retrieve top 10 chunks
```

## Next Steps
- Add user authentication
- Implement document deletion
- Add support for more file formats (DOCX, etc.)
- Implement conversation history
- Add streaming responses
- Deploy to production

## License
MIT

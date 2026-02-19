# Development Setup Guide

## Prerequisites

Before setting up MediSureAI locally, ensure all required software is installed:

### Required Software

| Software | Minimum Version | Recommended Version | Purpose |
|----------|----------------|---------------------|---------|
| **Java JDK** | 17 | 21 | Backend runtime |
| **Maven** | 3.8 | 3.9+ | Build tool |
| **Node.js** | 18.x | 20.x LTS | Frontend runtime |
| **npm** | 9.x | 10.x | JavaScript package manager |
| **PostgreSQL** | 15.0 | 16.0 | Relational database |
| **Docker** | 20.10 | 24.x | Container runtime (for ChromaDB) |
| **Docker Compose** | 2.0 | 2.x | Multi-container orchestration |
| **Git** | 2.30 | Latest | Version control |

### Optional but Recommended

- **Postman** or **Insomnia**: For API testing
- **DBeaver** or **pgAdmin**: PostgreSQL GUI client
- **VS Code**: Recommended IDE with Java Extension Pack and ES7 React snippets

### Verify Installation

Run these commands to verify software installation:

```bash
java -version        # Should show Java 17+
mvn -version         # Should show Maven 3.8+
node -version        # Should show Node 18+
npm -version         # Should show npm 9+
psql --version       # Should show PostgreSQL 15+
docker --version     # Should show Docker 20.10+
docker compose version  # Should show Docker Compose 2.0+
```

---

## Backend Local Setup

### Step 1: Clone Repository

```bash
cd /path/to/your/workspace
git clone https://github.com/your-org/medisure-ai.git
cd medisure-ai/backend
```

### Step 2: Configure PostgreSQL Database

#### Create Database

```bash
# Connect to PostgreSQL as superuser
psql -U postgres

# Create database and user
CREATE DATABASE medisure_db;
CREATE USER medisure_user WITH ENCRYPTED PASSWORD 'medisure_password';
GRANT ALL PRIVILEGES ON DATABASE medisure_db TO medisure_user;

# Grant additional privileges (PostgreSQL 15+)
\c medisure_db
GRANT ALL ON SCHEMA public TO medisure_user;

# Exit psql
\q
```

#### Verify Database Connection

```bash
psql -U medisure_user -d medisure_db -h localhost -p 5432
# Enter password: medisure_password
```

### Step 3: Configure Environment Variables

Create `.env` file in `backend/` directory:

```bash
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=medisure_db
POSTGRES_USER=medisure_user
POSTGRES_PASSWORD=medisure_password

# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=86400000

# AI Configuration
OPENAI_API_KEY=sk-proj-your-openai-api-key
AI_PROVIDER=openai
AI_MODEL=gpt-4

# Ollama Configuration (if using local LLM)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3:70b

# ChromaDB Configuration
CHROMA_HOST=localhost
CHROMA_PORT=8000
CHROMA_COLLECTION=medisure_embeddings

# Server Configuration
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# File Storage (local development)
FILE_STORAGE_PATH=./uploads
MAX_FILE_SIZE=10485760

# Logging
LOGGING_LEVEL=INFO
```

### Step 4: Install Dependencies

```bash
cd backend
mvn clean install -DskipTests
```

This will:
- Download all Maven dependencies
- Compile Java source files
- Package application (but skip tests for faster initial setup)

### Step 5: Run Database Migrations

Spring Boot auto-creates tables from JPA entities on first run. Alternatively, use Flyway for versioned migrations:

```bash
# If using Flyway (optional), place migration scripts in:
# src/main/resources/db/migration/V1__initial_schema.sql

mvn flyway:migrate
```

### Step 6: Run Backend Application

```bash
# Using Maven
mvn spring-boot:run

# OR using JAR (after mvn package)
java -jar target/medisure-backend-1.0.0.jar
```

**Expected Output**:
```
  __  __          _ _  _____                
 |  \/  |        | (_)/ ____|               
 | \  / | ___  __| |_| (___  _   _ _ __ ___ 
 | |\/| |/ _ \/ _` | |\___ \| | | | '__/ _ \
 | |  | |  __/ (_| | |____) | |_| | | |  __/
 |_|  |_|\___|\__,_|_|_____/ \__,_|_|  \___|
                                             
2026-02-20 10:30:45.123  INFO 12345 --- [main] c.m.MediSureApplication : Starting MediSureApplication
2026-02-20 10:30:47.456  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2026-02-20 10:30:47.462  INFO 12345 --- [main] c.m.MediSureApplication : Started MediSureApplication in 3.245 seconds
```

### Step 7: Verify Backend is Running

```bash
# Health check endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### Step 8: Create Admin User (Optional)

Run SQL script to create default admin user:

```sql
-- Connect to database
psql -U medisure_user -d medisure_db

-- Insert admin user (password: Admin@123)
INSERT INTO users (id, name, email, password_hash, role, created_at, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'System Admin',
  'admin@medisure.ai',
  '$2a$10$encrypted_password_hash_here',
  'ADMIN',
  NOW(),
  NOW()
);
```

**Note**: Use BCrypt password encoder. Generate hash in Spring Boot:
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode("Admin@123");
System.out.println(hashedPassword);
```

---

## Frontend Local Setup

### Step 1: Navigate to Frontend Directory

```bash
cd /path/to/medisure-ai/frontend
```

### Step 2: Configure Environment Variables

Create `.env` file in `frontend/` directory:

```bash
# API Configuration
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_API_TIMEOUT=30000

# Feature Flags
VITE_ENABLE_ANALYTICS=false
VITE_ENABLE_DEBUG_MODE=true

# Third-party Services (optional in development)
VITE_SENTRY_DSN=
VITE_GOOGLE_ANALYTICS_ID=
```

### Step 3: Install Dependencies

```bash
npm install
```

This installs all packages from `package.json`:
- React, React Router
- Zustand (state management)
- Axios (HTTP client)
- Tailwind CSS (styling)
- Vite plugins

### Step 4: Run Frontend Development Server

```bash
npm run dev
```

**Expected Output**:
```
  VITE v5.0.10  ready in 1234 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### Step 5: Access Application

Open browser and navigate to: **http://localhost:5173**

You should see the MediSureAI login page.

### Step 6: Build for Production (Optional)

```bash
npm run build

# Preview production build
npm run preview
```

Build artifacts will be in `dist/` directory.

---

## ChromaDB Vector Database Setup

ChromaDB is used for storing document embeddings for RAG pipeline.

### Option 1: Docker Compose (Recommended)

Create `docker-compose.yml` in project root:

```yaml
version: '3.8'

services:
  chromadb:
    image: chromadb/chroma:latest
    container_name: medisure-chromadb
    ports:
      - "8000:8000"
    volumes:
      - chromadb-data:/chroma/data
    environment:
      - IS_PERSISTENT=TRUE
      - ANONYMIZED_TELEMETRY=FALSE
    restart: unless-stopped

volumes:
  chromadb-data:
    driver: local
```

**Start ChromaDB**:
```bash
docker compose up -d chromadb
```

**Verify ChromaDB is Running**:
```bash
curl http://localhost:8000/api/v1/heartbeat

# Expected response:
# {"nanosecond heartbeat": 1708356123456789}
```

**View ChromaDB Logs**:
```bash
docker compose logs -f chromadb
```

**Stop ChromaDB**:
```bash
docker compose down
```

### Option 2: Local Installation (Alternative)

```bash
# Install ChromaDB Python package
pip install chromadb

# Run ChromaDB server
chroma run --path ./chroma-data --port 8000
```

### ChromaDB Collections

MediSureAI uses a single collection: `medisure_embeddings`

Collection is auto-created on first document ingestion with configuration:
- **Distance Metric**: Cosine similarity
- **Embedding Model**: OpenAI text-embedding-ada-002 (1536 dimensions)
- **Index**: HNSW (Hierarchical Navigable Small World)

---

## Environment Variable Configuration Guide

### Backend Environment Variables Reference

#### Database Configuration

```bash
# PostgreSQL connection
POSTGRES_HOST=localhost          # Database host
POSTGRES_PORT=5432               # Database port
POSTGRES_DB=medisure_db          # Database name
POSTGRES_USER=medisure_user      # Database user
POSTGRES_PASSWORD=medisure_password  # Database password
```

#### Security Configuration

```bash
# JWT token configuration
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters-long
# Generate random secret: openssl rand -base64 32
JWT_EXPIRATION=900000            # Access token expiration (15 min in ms)
JWT_REFRESH_EXPIRATION=86400000  # Refresh token expiration (1 day in ms)
```

#### AI Configuration

**For OpenAI**:
```bash
AI_PROVIDER=openai
AI_MODEL=gpt-4
OPENAI_API_KEY=sk-proj-your-openai-api-key
OPENAI_EMBEDDING_MODEL=text-embedding-ada-002
```

**For Ollama (Local LLM)**:
```bash
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3:70b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
```

#### ChromaDB Configuration

```bash
CHROMA_HOST=localhost
CHROMA_PORT=8000
CHROMA_COLLECTION=medisure_embeddings
```

#### Server Configuration

```bash
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

#### File Storage

```bash
FILE_STORAGE_PATH=./uploads
MAX_FILE_SIZE=10485760  # 10MB in bytes
```

#### Logging

```bash
LOGGING_LEVEL=INFO  # Options: TRACE, DEBUG, INFO, WARN, ERROR
```

### Frontend Environment Variables Reference

```bash
# API Configuration
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_API_TIMEOUT=30000

# Feature Flags
VITE_ENABLE_ANALYTICS=false
VITE_ENABLE_DEBUG_MODE=true
```

---

## How to Run Document Ingestion

Document ingestion processes policy PDFs and medical guidelines into ChromaDB for RAG retrieval.

### Method 1: Via API (Recommended)

Upload policy document via REST API:

```bash
curl -X POST http://localhost:8080/api/v1/policies/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "policyFile=@/path/to/policy.pdf" \
  -F "policyNumber=POL-2026-12345" \
  -F "providerName=Max Bupa Health Insurance" \
  -F "planName=Premium Health Plus" \
  -F "coverageAmount=500000" \
  -F "premium=15000" \
  -F "startDate=2026-01-01" \
  -F "expiryDate=2027-01-01"
```

**Response**:
```json
{
  "success": true,
  "message": "Policy uploaded successfully. Document ingestion in progress.",
  "data": {
    "policyId": "661e8400-e29b-41d4-a716-446655440002",
    "ingestionStatus": "PROCESSING"
  }
}
```

**Check Ingestion Status**:
```bash
curl -X GET http://localhost:8080/api/v1/policies/661e8400.../status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Method 2: Via Postman/Insomnia

1. Open Postman
2. Create new POST request to `http://localhost:8080/api/v1/policies/upload`
3. Set Authorization → Bearer Token → Paste JWT
4. Set Body → form-data
5. Add file field `policyFile` and select PDF
6. Add text fields for metadata (policyNumber, providerName, etc.)
7. Click Send

### Method 3: Via Frontend UI

1. Log in as PATIENT or ADMIN
2. Navigate to "My Policies" → "Upload New Policy"
3. Fill form with policy details
4. Select PDF file
5. Click "Upload"

### Ingestion Pipeline Steps

When document is uploaded:

**Step 1**: File uploaded to server (stored in `./uploads` directory)

**Step 2**: RAG ingestion service triggered asynchronously

**Step 3**: Document parsed using Apache Tika (PDF → raw text)

**Step 4**: Text preprocessed (clean, normalize, remove boilerplate)

**Step 5**: Text chunked (512 tokens per chunk, 50 token overlap)

**Step 6**: Chunks embedded using OpenAI text-embedding-ada-002

**Step 7**: Embeddings + metadata stored in ChromaDB collection

**Step 8**: Policy status updated from PROCESSING → ACTIVE

### Monitor Ingestion Progress

**View Backend Logs**:
```bash
# Tail logs for ingestion events
tail -f logs/application.log | grep "RAG"
```

**Expected Log Output**:
```
2026-02-20 10:45:12.123  INFO [RAGIngestionService] Starting ingestion for policy POL-2026-12345
2026-02-20 10:45:13.456  INFO [DocumentParser] Parsed 45 pages, 12,345 words
2026-02-20 10:45:14.789  INFO [ChunkingService] Created 78 chunks with 50-token overlap
2026-02-20 10:45:18.012  INFO [EmbeddingService] Generated embeddings for 78 chunks
2026-02-20 10:45:19.345  INFO [ChromaDBClient] Stored 78 embeddings in collection 'medisure_embeddings'
2026-02-20 10:45:19.678  INFO [RAGIngestionService] Ingestion completed for policy POL-2026-12345
```

### Verify Data in ChromaDB

```bash
# Query ChromaDB collection
curl -X POST http://localhost:8000/api/v1/collections/medisure_embeddings/query \
  -H "Content-Type: application/json" \
  -d '{
    "query_texts": ["diabetes coverage"],
    "n_results": 5
  }'
```

---

## How to Run Both Services Together

### Option 1: Separate Terminal Windows

**Terminal 1 (Backend)**:
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 (Frontend)**:
```bash
cd frontend
npm run dev
```

**Terminal 3 (ChromaDB)**:
```bash
docker compose up chromadb
```

### Option 2: Using Tmux (Linux/Mac)

```bash
# Start tmux session
tmux new-session -s medisure

# Split window horizontally
Ctrl+b "

# Split each pane vertically
Ctrl+b %

# Navigate panes
Ctrl+b arrow-keys

# In pane 1 (Backend)
cd backend && mvn spring-boot:run

# In pane 2 (Frontend)
cd frontend && npm run dev

# In pane 3 (ChromaDB)
docker compose up chromadb

# Detach from tmux session
Ctrl+b d

# Reattach later
tmux attach-session -t medisure
```

### Option 3: Docker Compose (All Services)

Create comprehensive `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: medisure-postgres
    environment:
      POSTGRES_DB: medisure_db
      POSTGRES_USER: medisure_user
      POSTGRES_PASSWORD: medisure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped

  chromadb:
    image: chromadb/chroma:latest
    container_name: medisure-chromadb
    ports:
      - "8000:8000"
    volumes:
      - chromadb-data:/chroma/data
    environment:
      - IS_PERSISTENT=TRUE
    restart: unless-stopped

  backend:
    build: ./backend
    container_name: medisure-backend
    ports:
      - "8080:8080"
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: medisure_db
      POSTGRES_USER: medisure_user
      POSTGRES_PASSWORD: medisure_password
      CHROMA_HOST: chromadb
      CHROMA_PORT: 8000
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      - postgres
      - chromadb
    restart: unless-stopped

  frontend:
    build: ./frontend
    container_name: medisure-frontend
    ports:
      - "5173:5173"
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api/v1
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres-data:
  chromadb-data:
```

**Start All Services**:
```bash
docker compose up -d
```

**View Logs**:
```bash
docker compose logs -f
```

**Stop All Services**:
```bash
docker compose down
```

---

## Common Issues and Fixes

### Issue 1: Port Already in Use

**Symptom**: `Port 8080 already in use` or `Port 5173 already in use`

**Fix**:
```bash
# Find process using port
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Kill process
kill -9 PID  # Mac/Linux
taskkill /PID PID /F  # Windows

# OR change port in configuration
# Backend: change SERVER_PORT in .env
# Frontend: change port in vite.config.js
```

### Issue 2: PostgreSQL Connection Refused

**Symptom**: `Connection refused` or `FATAL: password authentication failed`

**Fix**:
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql  # Linux
brew services list | grep postgresql  # Mac

# Check PostgreSQL logs
tail -f /var/log/postgresql/postgresql-16-main.log

# Verify credentials
psql -U medisure_user -d medisure_db -h localhost -p 5432

# Reset password if needed
sudo -u postgres psql
ALTER USER medisure_user WITH PASSWORD 'medisure_password';
```

### Issue 3: ChromaDB Not Accessible

**Symptom**: `Connection refused to localhost:8000`

**Fix**:
```bash
# Check Docker container is running
docker ps | grep chromadb

# Check ChromaDB logs
docker logs medisure-chromadb

# Restart ChromaDB container
docker restart medisure-chromadb

# Verify ChromaDB health
curl http://localhost:8000/api/v1/heartbeat
```

### Issue 4: OpenAI API Key Invalid

**Symptom**: `401 Unauthorized` from OpenAI API

**Fix**:
- Verify API key is correct in `.env`
- Check OpenAI account has credits
- Ensure no extra spaces/newlines in API key
- Test API key directly:
  ```bash
  curl https://api.openai.com/v1/models \
    -H "Authorization: Bearer YOUR_API_KEY"
  ```

### Issue 5: Maven Dependencies Not Downloading

**Symptom**: `Could not resolve dependencies` or `Plugin not found`

**Fix**:
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Force update dependencies
mvn clean install -U

# Use verbose mode to see what's failing
mvn clean install -X
```

### Issue 6: Frontend npm Install Fails

**Symptom**: `npm ERR! code ERESOLVE` or `peer dependency conflict`

**Fix**:
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall with legacy peer deps
npm install --legacy-peer-deps

# OR use npm force
npm install --force
```

### Issue 7: CORS Error in Browser

**Symptom**: `Access to XMLHttpRequest blocked by CORS policy`

**Fix**:
- Verify `CORS_ALLOWED_ORIGINS` in backend `.env` includes `http://localhost:5173`
- Check `SecurityConfig.java` has correct CORS configuration
- Clear browser cache and cookies
- Try different browser or incognito mode

### Issue 8: JWT Token Expired

**Symptom**: `401 Unauthorized` after some time, `JWT token has expired`

**Fix**:
- Token expires after 15 minutes (configurable)
- Frontend should automatically refresh token using refresh token
- Check `authStore.js` has token refresh logic
- Manually refresh token:
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/refresh \
    -H "Content-Type: application/json" \
    -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
  ```

### Issue 9: File Upload Fails

**Symptom**: `413 Payload Too Large` or `File upload failed`

**Fix**:
- Check file size < 10MB (configurable)
- Verify `MAX_FILE_SIZE` in backend `.env`
- Check `uploads/` directory exists and has write permissions:
  ```bash
  mkdir -p backend/uploads
  chmod 755 backend/uploads
  ```
- Verify file is valid PDF (not corrupted)

### Issue 10: Database Schema Outdated

**Symptom**: `Table doesn't exist` or `Column not found`

**Fix**:
```bash
# Drop and recreate database (WARNING: deletes all data)
psql -U postgres -c "DROP DATABASE medisure_db;"
psql -U postgres -c "CREATE DATABASE medisure_db;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE medisure_db TO medisure_user;"

# Restart backend (Spring Boot will recreate tables)
cd backend
mvn spring-boot:run
```

---

## Quick Start Summary

For impatient developers who want to get started quickly:

```bash
# 1. Start PostgreSQL and ChromaDB
docker compose up -d postgres chromadb

# 2. Configure environment variables
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
# Edit both .env files with your configuration

# 3. Start backend
cd backend
mvn spring-boot:run

# 4. In new terminal, start frontend
cd frontend
npm install
npm run dev

# 5. Access application
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080/api/v1
# ChromaDB: http://localhost:8000
```

**Default credentials** (after creating admin user):
- Email: `admin@medisure.ai`
- Password: `Admin@123`

---

## Next Steps

After successful setup:

1. **Test API endpoints**: Use Postman collection (import from `docs/api/postman-collection.json`)
2. **Upload sample policy**: Test RAG pipeline with sample insurance policy PDF
3. **Create test users**: Register patient, doctor, admin accounts
4. **Submit test claim**: Test complete workflow from claim submission to decision
5. **Review logs**: Monitor application behavior and errors
6. **Read architecture docs**: Understand system design (`docs/architecture/`)

Happy coding! 🚀

# 🏥 MediSureAI Backend

AI-Powered Healthcare & Insurance Decision Support Platform - Spring Boot Backend

## 📋 Project Overview

MediSureAI is an Agentic RAG-based platform that provides intelligent decision support for healthcare and insurance workflows. The backend is built with Spring Boot and integrates:

- **Agentic AI Architecture**: Multi-tool orchestration for intelligent decision-making
- **RAG Pipeline**: Retrieval-Augmented Generation with vector search
- **Clinical Decision Support**: Treatment validation against medical guidelines
- **Insurance Claim Validation**: Automated claim processing with explainable AI
- **Role-Based Access**: Separate interfaces for Patients, Doctors, and Admins

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Programming Language |
| **Spring Boot** | 4.0.3 | Application Framework |
| **PostgreSQL** | 16 | Relational Database |
| **pgvector** | pg16 | Vector Search Extension |
| **Ollama** | Latest | Local LLM Runtime |
| **Maven** | 3.9+ | Build Tool |
| **Docker** | Latest | Containerization |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Spring Data JPA** | 3.x | ORM & Database Access |
| **Lombok** | Latest | Code Generation |

## ⚡ Quick Start (Docker)

### Prerequisites

- **Docker** 20.10+ and **Docker Compose** 2.0+
- **Java JDK** 21
- **Maven** 3.8+

### 1. Start Infrastructure Services

Start PostgreSQL with pgvector and Ollama:

```bash
# Navigate to Backend directory
cd Backend

# Start services in detached mode
docker compose up -d

# Check service health
docker compose ps

# View logs
docker compose logs -f
```

**Services Started:**
- PostgreSQL with pgvector → `localhost:5432`
- Ollama (LLM) → `localhost:11434`

### 2. Pull Ollama Model

```bash
# Pull the required LLM model
docker exec -it ollama ollama pull llama3:8b

# Verify model is available
docker exec -it ollama ollama list
```

### 3. Run the Backend Application

```bash
# Build and run with Maven
mvn clean install -DskipTests
mvn spring-boot:run

# OR using JAR
mvn package -DskipTests
java -jar target/Backend-0.0.1-SNAPSHOT.jar
```

### 4. Verify Backend is Running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

✅ **Backend is now running on** `http://localhost:8080`

---

## 🔧 Local Development Setup

### Step 1: Install Prerequisites

**Required Software:**
```bash
# Verify installations
java -version        # Should show Java 21
mvn -version        # Should show Maven 3.8+
docker --version    # Should show Docker 20.10+
psql --version      # Should show PostgreSQL client
```

### Step 2: Configure Environment Variables

Create `.env` file in the `Backend/` directory (optional, defaults are set in application.properties):

```bash
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=vectordb
POSTGRES_USER=testuser
POSTGRES_PASSWORD=testpwd

# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3:8b

# JWT Configuration (change in production!)
JWT_SECRET=your-256-bit-secret-key-change-this-in-production
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=86400000

# Server Configuration
SERVER_PORT=8080
```

### Step 3: Install Dependencies

```bash
mvn clean install -DskipTests
```

This will:
- Download all Maven dependencies
- Compile Java source files
- Create the application JAR

### Step 4: Database Initialization

The database schema is automatically created by Spring Boot JPA on first run. Tables are generated from entity classes.

**Optional**: Create initial admin user (SQL script):

```sql
-- Connect to PostgreSQL
docker exec -it pgvector psql -U testuser -d vectordb

-- Create admin user
INSERT INTO users (id, name, email, password_hash, role, created_at)
VALUES (
  gen_random_uuid(),
  'Admin User',
  'admin@medisure.ai',
  '$2a$10$encrypted_password_here',
  'ADMIN',
  NOW()
);
```

### Step 5: Run Application

**Maven Spring Boot Plugin:**
```bash
mvn spring-boot:run
```

**Java JAR:**
```bash
java -jar target/Backend-0.0.1-SNAPSHOT.jar
```

**With Hot Reload (DevTools):**
```bash
mvn spring-boot:run -Dspring-boot.run.fork=false
```

---

## 📁 Project Structure

```
Backend/
├── src/main/java/com/example/Backend/
│   ├── BackendApplication.java          # Main application entry point
│   ├── config/                          # Configuration classes
│   │   ├── SecurityConfig.java          # Spring Security setup
│   │   ├── JwtConfig.java              # JWT authentication
│   │   └── CorsConfig.java             # CORS configuration
│   ├── modules/
│   │   ├── auth/                        # Authentication module
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   └── dto/
│   │   ├── claim/                       # Insurance claim module
│   │   ├── treatment/                   # Treatment validation module
│   │   ├── policy/                      # Policy management module
│   │   └── decision/                    # AI decision engine module
│   ├── rag/                             # RAG pipeline components
│   │   ├── OllamaService.java          # LLM integration
│   │   ├── VectorSearchService.java    # pgvector search
│   │   └── EmbeddingService.java       # Text embeddings
│   └── utils/                           # Utility classes
├── src/main/resources/
│   ├── application.properties           # Application configuration
│   ├── static/                          # Static resources
│   └── templates/                       # Email/document templates
├── src/test/java/                       # Unit and integration tests
├── compose.yml                          # Docker Compose configuration
├── pom.xml                              # Maven dependencies
└── README.md                            # This file
```

---

## 🌐 API Documentation

### Base URL
```
Development: http://localhost:8080/api/v1
```

### Authentication

All API requests require JWT authentication except for login/registration.

**Login:**
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123"
}

Response:
{
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "...",
  "user": {
    "id": "uuid",
    "name": "John Doe",
    "email": "user@example.com",
    "role": "PATIENT"
  }
}
```

**Authenticated Request:**
```bash
GET /api/v1/claims/patient/{userId}
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### Key Endpoints

| Endpoint | Method | Description | Role |
|----------|--------|-------------|------|
| `/api/v1/auth/login` | POST | User login | Public |
| `/api/v1/auth/register` | POST | User registration | Public |
| `/api/v1/claims/submit` | POST | Submit insurance claim | Patient |
| `/api/v1/claims/patient/{id}` | GET | Get patient claims | Patient |
| `/api/v1/treatment/validate` | POST | Validate treatment plan | Doctor |
| `/api/v1/policy/analyze` | POST | Analyze policy coverage | Patient |
| `/api/v1/decision/explain` | POST | Get AI decision explanation | All |

See full API documentation in [docs/api/api-reference.md](../docs/api/api-reference.md)

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=BackendApplicationTests
```

### Run with Coverage
```bash
mvn clean test jacoco:report
```

Coverage report will be available at `target/site/jacoco/index.html`

---

## 🐳 Docker Commands

### Start Services
```bash
docker compose up -d
```

### Stop Services
```bash
docker compose down
```

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f pgvector
docker compose logs -f ollama
```

### Restart Single Service
```bash
docker compose restart pgvector
```

### Remove Volumes (Clean Start)
```bash
docker compose down -v
```

### Database Access
```bash
# Connect to PostgreSQL
docker exec -it pgvector psql -U testuser -d vectordb

# Ollama CLI
docker exec -it ollama ollama list
docker exec -it ollama ollama pull llama3:8b
```

---

## 🔍 Troubleshooting

### Issue: Port 5432 Already in Use
```bash
# Find process using port
netstat -ano | findstr :5432

# Option 1: Kill the process
taskkill /PID <process_id> /F

# Option 2: Change port in compose.yml
ports:
  - "5433:5432"  # Use 5433 on host
```

### Issue: Application Cannot Connect to Database
```bash
# Check if PostgreSQL is running
docker compose ps

# Check database logs
docker compose logs pgvector

# Verify connection manually
docker exec -it pgvector psql -U testuser -d vectordb
```

### Issue: Ollama Model Not Found
```bash
# Pull the model
docker exec -it ollama ollama pull llama3:8b

# List available models
docker exec -it ollama ollama list
```

### Issue: Maven Build Fails
```bash
# Clean and rebuild
mvn clean install -U -DskipTests

# Clear local Maven cache if needed
rmdir /s %USERPROFILE%\.m2\repository\com\example
```

### Issue: Out of Memory
```bash
# Increase Docker memory limit (Docker Desktop → Settings → Resources)
# OR add environment variable
set MAVEN_OPTS=-Xmx2048m -XX:MaxPermSize=512m
mvn spring-boot:run
```

---

## 📊 Database Schema

The application uses Spring Data JPA to auto-generate tables. Key entities:

- **users**: User accounts (Patient, Doctor, Admin)
- **claims**: Insurance claims
- **treatments**: Treatment plans
- **policies**: Insurance policies
- **decisions**: AI decision records
- **embeddings**: Vector embeddings for RAG

To view schema:
```sql
docker exec -it pgvector psql -U testuser -d vectordb
\dt  -- List tables
\d users  -- Describe users table
```

---

## 🚀 Deployment

### Production Build
```bash
mvn clean package -Pprod
```

### Run Production JAR
```bash
java -jar -Dspring.profiles.active=prod target/Backend-0.0.1-SNAPSHOT.jar
```

### Environment Variables for Production
```bash
# Set production environment variables
export SPRING_PROFILES_ACTIVE=prod
export POSTGRES_HOST=your-prod-db-host
export JWT_SECRET=your-secure-256-bit-secret
export OLLAMA_BASE_URL=https://your-ollama-endpoint
```

---

## 📚 Additional Documentation

- [System Architecture](../docs/architecture/system-architecture.md)
- [Backend Modules](../docs/modules/backend-modules.md)
- [RAG Pipeline](../docs/architecture/rag-pipeline.md)
- [API Reference](../docs/api/api-reference.md)
- [Testing Strategy](../docs/testing/testing-strategy.md)
- [Development Setup](../docs/setup/dev-setup.md)

---

## 🤝 Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make changes and test thoroughly
3. Commit with clear messages: `git commit -m "Add: feature description"`
4. Push and create pull request

---

## 📝 License

MediSureAI Backend - Spring Boot Application  
Version: 0.0.1-SNAPSHOT

---

## 💡 Support

For issues or questions:
- Check [Troubleshooting](#-troubleshooting) section
- Review [Documentation](../docs/)
- Contact: dev@medisure.ai
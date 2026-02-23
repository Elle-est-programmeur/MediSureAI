@echo off
echo ========================================
echo MediSure AI - RAG Application Setup
echo ========================================
echo.

echo Step 1: Starting Docker services...
cd Backend
docker-compose -f compose.yml up -d
if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to start Docker services
    pause
    exit /b 1
)
echo Docker services started successfully!
echo.

echo Step 2: Waiting for services to be ready...
timeout /t 10 /nobreak
echo.

echo Step 3: Pulling Ollama models...
echo This may take a few minutes...
docker exec ollama ollama pull llama3.2:3b
docker exec ollama ollama pull nomic-embed-text
echo Ollama models pulled successfully!
echo.

echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Start the Backend:
echo    cd Backend
echo    mvnw.cmd spring-boot:run
echo.
echo 2. Start the Frontend (in a new terminal):
echo    cd Frontend
echo    npm install
echo    npm run dev
echo.
echo 3. Open your browser at http://localhost:5173
echo.
pause

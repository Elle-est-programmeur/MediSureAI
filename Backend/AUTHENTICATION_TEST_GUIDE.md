# Authentication Test Guide

How to exercise the JWT authentication flow for the MediSureAI backend.

## Overview
- **Access token** (`jwt.access.expiration`) + **refresh token** (7 days), with rotation on refresh.
- Roles: **PATIENT** (default on register), **DOCTOR**, **ADMIN**.
- Public endpoints: `POST /auth/register`, `/auth/login`, `/auth/refresh`.
- Everything else requires `Authorization: Bearer <accessToken>`.

> There is no seeded default user — register one first.

## Prerequisites
1. Data services running: `docker compose up -d medsureai-db pgvector mongodb rabbitmq ollama`
2. Backend running on port 8080: `./mvnw spring-boot:run`

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | Public | Register a user (default role PATIENT) |
| POST | `/auth/login` | Public | Login, returns access + refresh tokens |
| POST | `/auth/refresh` | Public | Rotate refresh token, get new access token |
| POST | `/auth/logout` | Bearer | Invalidate the caller's refresh token |
| GET | `/api/documents` | Bearer | Example protected endpoint |
| POST | `/api/chat/ask` | Bearer | Example protected endpoint |

### Request bodies
```jsonc
// POST /auth/register   (role optional; defaults to PATIENT, may be DOCTOR/ADMIN)
{ "username": "patient1", "email": "patient1@test.com", "password": "test123", "role": "PATIENT" }

// POST /auth/login
{ "username": "patient1", "password": "test123" }

// POST /auth/refresh
{ "refreshToken": "<refresh-token>" }
```

### Response (`AuthResponse`)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tokenType": "Bearer",
  "username": "patient1",
  "email": "patient1@test.com",
  "role": "PATIENT"
}
```

## Testing with PowerShell

```powershell
# 1. Register
$registerBody = @{ username = "patient1"; email = "patient1@test.com"; password = "test123" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
$accessToken  = $response.accessToken
$refreshToken = $response.refreshToken

# 2. Login (same credentials)
$loginBody = @{ username = "patient1"; password = "test123" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$accessToken  = $response.accessToken
$refreshToken = $response.refreshToken

# 3. Call a protected endpoint
$headers = @{ Authorization = "Bearer $accessToken" }
Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -Method Get -Headers $headers

# 4. Refresh tokens (rotation: old refresh token is invalidated)
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/refresh" -Method Post -Body $refreshBody -ContentType "application/json"
$accessToken  = $response.accessToken
$refreshToken = $response.refreshToken

# 5. Logout
Invoke-RestMethod -Uri "http://localhost:8080/auth/logout" -Method Post -Headers @{ Authorization = "Bearer $accessToken" }

# 6. Unauthorized request should fail with 401
try { Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -Method Get }
catch { Write-Host "Got expected 401:" $_.Exception.Response.StatusCode }
```

## Testing with cURL

```bash
# Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"patient1","email":"patient1@test.com","password":"test123"}'

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patient1","password":"test123"}'

# Protected endpoint
curl http://localhost:8080/api/documents -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Refresh
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'

# Logout
curl -X POST http://localhost:8080/auth/logout -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Expected Results

| Scenario | Status |
|----------|--------|
| Register / login success | `200 OK` with `AuthResponse` |
| Missing/invalid token on protected endpoint | `401 Unauthorized` |
| Valid token, wrong role (e.g. PATIENT hitting `/api/doctor/**`) | `403 Forbidden` |
| Duplicate username/email on register | `409 Conflict` |
| Validation failure (short password, bad email) | `400 Bad Request` |

## Troubleshooting
- **Cannot connect to database** — ensure the `medsureai-db` container is up: `docker compose ps`.
- **Invalid/expired token** — access tokens are short-lived; use `/auth/refresh` or log in again.
- **Refresh token expired** — log in again to get a fresh pair.
- **Port 8080 in use** — `netstat -ano | findstr :8080`, then `taskkill /PID <pid> /F`.

## Security Notes
- Set a strong, base64-encoded 256-bit `JWT_SECRET_KEY` via environment variable; never commit it.
- Passwords are hashed with BCrypt (strength 12).
- Use HTTPS and restrict CORS origins in production (currently permissive for local development).

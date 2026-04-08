# Authentication System Test Guide

## Overview
JWT-based authentication system with access tokens (15 min) and refresh tokens (7 days) for MediSureAI.

## Default Credentials
- **Admin User**: `admin` / `admin123`
- **Email**: admin@medisureai.com

## Prerequisites
1. PostgreSQL running in Docker: `docker compose up -d`
2. Spring Boot application running on port 8080

## Testing with PowerShell

### 1. Register a New Patient User
```powershell
$registerBody = @{
    username = "patient1"
    email = "patient1@test.com"
    password = "test123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
    -Method Post `
    -Body $registerBody `
    -ContentType "application/json"

Write-Host "Registration successful!"
Write-Host "Access Token: $($response.accessToken)"
Write-Host "Refresh Token: $($response.refreshToken)"
Write-Host "Username: $($response.username)"
Write-Host "Role: $($response.role)"

# Save tokens for later use
$accessToken = $response.accessToken
$refreshToken = $response.refreshToken
```

### 2. Login with Existing User
```powershell
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method Post `
    -Body $loginBody `
    -ContentType "application/json"

Write-Host "Login successful!"
Write-Host "Access Token: $($response.accessToken)"
Write-Host "Refresh Token: $($response.refreshToken)"
Write-Host "Username: $($response.username)"
Write-Host "Role: $($response.role)"

# Save tokens
$accessToken = $response.accessToken
$refreshToken = $response.refreshToken
```

### 3. Access Protected Endpoint (Get Documents)
```powershell
# Use the access token from login/register
$headers = @{
    "Authorization" = "Bearer $accessToken"
}

$documents = Invoke-RestMethod -Uri "http://localhost:8080/api/documents" `
    -Method Get `
    -Headers $headers

Write-Host "Documents retrieved successfully!"
$documents | ConvertTo-Json
```

### 4. Test Chat Endpoint (Protected)
```powershell
$chatBody = @{
    question = "What is MediSureAI?"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

$chatResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/chat/ask" `
    -Method Post `
    -Body $chatBody `
    -Headers $headers

Write-Host "Chat response received!"
$chatResponse | ConvertTo-Json
```

### 5. Refresh Access Token
```powershell
$refreshBody = @{
    refreshToken = $refreshToken
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/refresh" `
    -Method Post `
    -Body $refreshBody `
    -ContentType "application/json"

Write-Host "Token refreshed successfully!"
Write-Host "New Access Token: $($response.accessToken)"
Write-Host "New Refresh Token: $($response.refreshToken)"

# Update tokens
$accessToken = $response.accessToken
$refreshToken = $response.refreshToken
```

### 6. Logout
```powershell
$headers = @{
    "Authorization" = "Bearer $accessToken"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/logout" `
    -Method Post `
    -Headers $headers

Write-Host "Logged out successfully!"
```

### 7. Test Unauthorized Access (Should Fail)
```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/documents" -Method Get
    Write-Host "ERROR: Should have received 401 Unauthorized!"
} catch {
    Write-Host "Correctly received 401 Unauthorized" -ForegroundColor Green
    $_.Exception.Response.StatusCode
}
```

## Testing with cURL

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"patient1\",\"email\":\"patient1@test.com\",\"password\":\"test123\"}"
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

### Access Protected Endpoint
```bash
# Replace YOUR_ACCESS_TOKEN with actual token
curl http://localhost:8080/api/documents \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"YOUR_REFRESH_TOKEN\"}"
```

### Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Testing with Postman

1. **Import Collection**: Create a new collection called "MediSureAI Auth"

2. **Set Environment Variables**:
   - `baseUrl`: `http://localhost:8080`
   - `accessToken`: (will be set automatically)
   - `refreshToken`: (will be set automatically)

3. **Register Request**:
   - Method: POST
   - URL: `{{baseUrl}}/api/auth/register`
   - Body (JSON):
     ```json
     {
       "username": "patient1",
       "email": "patient1@test.com",
       "password": "test123"
     }
     ```
   - Tests (to save tokens):
     ```javascript
     pm.environment.set("accessToken", pm.response.json().accessToken);
     pm.environment.set("refreshToken", pm.response.json().refreshToken);
     ```

4. **Login Request**:
   - Same as Register, just change endpoint to `/api/auth/login` and body to:
     ```json
     {
       "username": "admin",
       "password": "admin123"
     }
     ```

5. **Protected Endpoint Request**:
   - Method: GET
   - URL: `{{baseUrl}}/api/documents`
   - Authorization: Bearer Token
   - Token: `{{accessToken}}`

## Database Verification

Connect to PostgreSQL and verify tables:

```powershell
docker exec -it pgvector psql -U testuser -d vectordb
```

Then run:
```sql
-- Check users table
SELECT id, username, email, role, created_at FROM users;

-- Check refresh tokens
SELECT id, token, expiry_date, created_at FROM refresh_tokens;

-- Check if default admin exists
SELECT * FROM users WHERE username = 'admin';
```

## Expected Results

### Successful Registration
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "username": "patient1",
  "role": "PATIENT"
}
```

### Successful Login
Same structure as registration response.

### Unauthorized Access (401)
```json
{
  "timestamp": "2026-02-26T...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required...",
  "path": "/api/documents"
}
```

### Validation Error (400)
```json
{
  "timestamp": "2026-02-26T...",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "username": "Username is required",
    "email": "Email must be valid"
  },
  "path": "/api/auth/register"
}
```

### User Already Exists (409)
```json
{
  "timestamp": "2026-02-26T...",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists",
  "path": "/api/auth/register"
}
```

## Troubleshooting

### Issue: "Cannot connect to database"
**Solution**: Ensure PostgreSQL container is running:
```powershell
docker compose up -d
docker compose ps
```

### Issue: "Invalid JWT token"
**Solution**: Token may have expired (15 min). Use refresh token or login again.

### Issue: "Refresh token expired"
**Solution**: Login again to get new tokens.

### Issue: Maven wrapper fails
**Solution**: The mvnw scripts need PowerShell configured. Alternative: Use VS Code Java extension to run the application directly from BackendApplication.java (right-click > Run Java).

### Issue: Port 8080 already in use
**Solution**: Stop any running instance:
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

## Security Notes

1. **Default Admin Password**: Change `admin123` in production!
2. **JWT Secret**: Update `jwt.secret` in application.properties for production
3. **Token Expiration**: 
   - Access tokens: 15 minutes (secure, requires refresh)
   - Refresh tokens: 7 days (balance between security and UX)
4. **HTTPS**: Use HTTPS in production, not HTTP
5. **CORS**: Update allowed origins in SecurityConfig for production domains

## Next Steps

1. Test all endpoints listed above
2. Verify database tables are created correctly
3. Test token expiration and refresh mechanism
4. Add role-based authorization (`@PreAuthorize`) to specific endpoints
5. Implement password reset functionality
6. Add email verification for new users
7. Update frontend to use authentication

## API Endpoints Summary

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/api/auth/register` | POST | ❌ No | Register new user (default: PATIENT) |
| `/api/auth/login` | POST | ❌ No | Login existing user |
| `/api/auth/refresh` | POST | ❌ No | Refresh access token |
| `/api/auth/logout` | POST | ✅ Yes | Logout (invalidate refresh token) |
| `/api/documents` | GET | ✅ Yes | Get all documents |
| `/api/documents/upload` | POST | ✅ Yes | Upload document |
| `/api/chat/ask` | POST | ✅ Yes | Ask question to AI |

---

**Implementation Complete!** 🎉

All authentication components have been created and configured. The system uses:
- ✅ JWT tokens with BCrypt password encryption
- ✅ Access + Refresh token mechanism
- ✅ Role-based system (PATIENT, ADMIN)
- ✅ Global exception handling
- ✅ Default admin user on startup
- ✅ All endpoints protected (except auth endpoints)

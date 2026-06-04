# Frontend Startup Troubleshooting

Quick reference for common errors when starting the MediSureAI frontend (Vite + React 19).

**Normal start:**
```powershell
cd C:\Users\shrey\MediSureAI\Frontend
npm run dev
```
Expected: Vite prints `VITE v7.x ready` and serves on `http://localhost:5173`.

---

## 1. `npm run dev` exits immediately / silently (exit code 1, no error)

**Symptom:** The npm script prints the header and nothing else, then the process exits. No stack trace.

```
> frontend@0.0.0 dev
> vite
```

**Cause:** Seen when launching through a detached/background shell or a non-interactive wrapper — the npm `.cmd` shim spawns `vite` and then the parent exits, taking the child with it.

**Fix:** Run Vite directly (bypasses the npm shim):
```powershell
node .\node_modules\vite\bin\vite.js --host
```
For normal interactive use in a real terminal, `npm run dev` works fine — this only bites in background/non-interactive launches.

**Verify it's actually listening:**
```powershell
Get-NetTCPConnection -State Listen -LocalPort 5173 -ErrorAction SilentlyContinue
```

---

## 2. `'vite' is not recognized` / `Cannot find module`

**Cause:** Dependencies not installed.

**Fix:**
```powershell
npm install
```
If `node_modules` looks corrupted or partial:
```powershell
Remove-Item -Recurse -Force node_modules, package-lock.json
npm install
```

---

## 3. Port 5173 already in use

**Symptom:** `Port 5173 is in use` (Vite auto-bumps to 5174) or a stale server is serving old code.

**Fix — find and kill the process holding the port:**
```powershell
$pid = (Get-NetTCPConnection -State Listen -LocalPort 5173).OwningProcess
Stop-Process -Id $pid -Force
```

---

## 4. Node version errors (`Unsupported engine` / syntax errors in Vite)

**Cause:** Vite 7 requires **Node 20.19+ or 22.12+**.

**Fix — check version:**
```powershell
node --version
```
This project is verified on **Node v22.14.0**. Upgrade Node if older.

---

## 5. API calls fail / CORS / 403 / network errors in the browser

The frontend talks to the backend on **http://localhost:8080**. If pages load but data doesn't:

- **Backend not running** — start it: `cd ..\Backend; .\mvnw.cmd spring-boot:run`. It's healthy when the log shows `Started BackendApplication`.
- **401 / 403** — expected for unauthenticated requests; log in first. Role-restricted endpoints (patient timeline, formulary) require the correct role (PATIENT vs DOCTOR).
- **Connection refused on :8080** — backend still booting (takes ~15–20s) or crashed; check its log.
- **Docker services down** — backend needs `mongodb`, `rabbitmq`, `medsure-pg` (Postgres on host port **5433**). Check: `docker ps`.
- **Ollama errors in backend** — SRLM/embeddings need Ollama on `localhost:11434`. Start it: `Start-Process "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe" serve`. Models needed: `llama3.2:1b`, `nomic-embed-text` (`ollama list` to confirm).

---

## 6. Blank white page (server runs, nothing renders)

- Open browser DevTools → Console for the real error.
- Common causes: a JS import error, or a crash in `App.jsx` / a route component.
- Hard refresh to clear stale cached bundle: **Ctrl+Shift+R**.
- Restart Vite to clear its cache:
  ```powershell
  Remove-Item -Recurse -Force node_modules\.vite -ErrorAction SilentlyContinue
  npm run dev
  ```

---

## 7. Tailwind styles not applying

This project uses **Tailwind v4** via `@tailwindcss/vite`. If styles are missing:
- Restart the dev server (Tailwind v4 picks up config on start).
- Confirm the main CSS file is imported in `src/main.jsx` and contains `@import "tailwindcss";`.

---

## Quick health check (both servers)

```powershell
# Frontend
try { (Invoke-WebRequest http://127.0.0.1:5173/ -TimeoutSec 5 -UseBasicParsing).StatusCode } catch { $_.Exception.Message }
# Backend (403 = up but unauthenticated, which is fine)
try { (Invoke-WebRequest http://localhost:8080/ -TimeoutSec 5 -UseBasicParsing).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }
```

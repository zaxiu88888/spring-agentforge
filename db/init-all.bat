@echo off
REM AgentForge one-shot DB init (idempotent). Requires local PostgreSQL on 5432.
setlocal
set PGHOME=E:\PostgreSQL\Library\bin
set PGPASSWORD=%PG_PASSWORD%
if "%PGPASSWORD%"=="" set PGPASSWORD=123456
set SCRIPT_DIR=%~dp0

echo [1/5] Ensure databases exist: agenthub agenthub_test eshop eshop_test
for %%D in (agenthub agenthub_test eshop eshop_test) do (
  "%PGHOME%\psql.exe" -h localhost -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='%%D'" | findstr 1 >nul
  if errorlevel 1 "%PGHOME%\psql.exe" -h localhost -U postgres -d postgres -c "CREATE DATABASE %%D"
)

echo [2/5] Apply schema.sql to agenthub and agenthub_test
for %%D in (agenthub agenthub_test) do "%PGHOME%\psql.exe" -h localhost -U postgres -d %%D -v ON_ERROR_STOP=1 -f "%SCRIPT_DIR%schema.sql"

echo [3/5] Apply eshop.sql to eshop and eshop_test
for %%D in (eshop eshop_test) do "%PGHOME%\psql.exe" -h localhost -U postgres -d %%D -v ON_ERROR_STOP=1 -f "%SCRIPT_DIR%eshop.sql"

echo [4/5] Create readonly role eshop_ro (default password eshop_ro_local)
"%PGHOME%\psql.exe" -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -c "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='eshop_ro') THEN CREATE ROLE eshop_ro LOGIN PASSWORD 'eshop_ro_local'; END IF; END $$;"
"%PGHOME%\psql.exe" -h localhost -U postgres -d eshop -v ON_ERROR_STOP=1 -c "GRANT USAGE ON SCHEMA public TO eshop_ro; GRANT SELECT ON ALL TABLES IN SCHEMA public TO eshop_ro; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO eshop_ro;"
"%PGHOME%\psql.exe" -h localhost -U postgres -d eshop_test -v ON_ERROR_STOP=1 -c "GRANT USAGE ON SCHEMA public TO eshop_ro; GRANT SELECT ON ALL TABLES IN SCHEMA public TO eshop_ro; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO eshop_ro;"

echo [5/5] Done. Connections:
echo   agenthub: jdbc:postgresql://localhost:5432/agenthub  (postgres)
echo   eshop:    jdbc:postgresql://localhost:5432/eshop     (eshop_ro / eshop_ro_local, readonly)
endlocal

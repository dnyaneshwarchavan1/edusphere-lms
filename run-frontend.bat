@echo off
cd /d "%~dp0frontend"
if not exist "node_modules" (
  echo Installing frontend dependencies...
  npm.cmd install
)
echo.
echo Starting EduSphere LMS frontend on http://localhost:5173
echo Keep this window open.
echo.
npm.cmd run dev
pause

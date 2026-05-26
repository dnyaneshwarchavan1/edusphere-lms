@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0backend"

if exist ".env" (
  echo Loading backend\.env database settings...
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%B"
  )
) else (
  echo backend\.env was not found. Using defaults from application.yml.
  echo Copy backend\.env.example to backend\.env and add DB plus Razorpay settings.
)

where java >nul 2>nul
if errorlevel 1 (
  echo Java is not installed or not available in PATH.
  echo Install Java 21 or newer, then run this file again.
  pause
  exit /b 1
)
echo Stopping old Java processes that may lock the backend build...
taskkill /F /IM java.exe >nul 2>nul
echo Building latest backend code...
call ..\mvnw.cmd package -DskipTests
if errorlevel 1 (
  echo.
  echo Backend build failed.
  pause
  exit /b 1
)
echo.
echo Starting EduSphere LMS backend on http://localhost:8080
echo MySQL database URL: !DB_URL!
echo MySQL username: !DB_USERNAME!
if defined STORAGE_PROVIDER (
  echo Storage provider: !STORAGE_PROVIDER!
) else (
  echo Storage provider: local
)
if defined RAZORPAY_KEY_ID (
  echo Razorpay key loaded: !RAZORPAY_KEY_ID:~0,12!...
) else (
  echo Razorpay key not loaded. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to backend\.env
)
if /I "!STORAGE_PROVIDER!"=="cloudinary" (
  if defined CLOUDINARY_CLOUD_NAME (
    echo Cloudinary cloud loaded: !CLOUDINARY_CLOUD_NAME!
  ) else (
    echo Cloudinary storage selected but CLOUDINARY_CLOUD_NAME is missing.
  )
)
echo Keep this window open.
echo Wait until you see:
echo Tomcat started on port 8080
echo Started LmsApplication
echo.
java -jar target\lms-0.0.1-SNAPSHOT.jar
if errorlevel 1 (
  echo.
  echo Backend stopped because of an error.
)
pause

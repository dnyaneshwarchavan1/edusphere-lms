@echo off
setlocal
set SCRIPT_DIR=%~dp0
set BUNDLED_MAVEN=%SCRIPT_DIR%tools\apache-maven-3.9.9\bin\mvn.cmd

if exist "%BUNDLED_MAVEN%" (
  call "%BUNDLED_MAVEN%" %*
  exit /b %errorlevel%
)

where mvn >nul 2>nul
if %errorlevel%==0 (
  call mvn %*
  exit /b %errorlevel%
)

echo Maven was not found.
echo Expected bundled Maven at:
echo %BUNDLED_MAVEN%
echo.
echo Install Maven or restore the tools\apache-maven-3.9.9 folder.
exit /b 1

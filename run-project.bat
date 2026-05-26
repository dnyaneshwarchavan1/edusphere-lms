@echo off
cd /d "%~dp0"
echo Starting EduSphere LMS project...
echo.
start "EduSphere Backend" cmd /k "%~dp0run-backend.bat"
echo.
echo Backend window opened.
echo Wait until backend shows:
echo Tomcat started on port 8080
echo Started LmsApplication
echo.
echo Then run:
echo run-frontend.bat
pause

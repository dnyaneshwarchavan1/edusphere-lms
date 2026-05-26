@echo off
cd /d "%~dp0backend"
call ..\mvnw.cmd clean package -DskipTests
pause

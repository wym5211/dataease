@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "PORT=%~1"
if "%PORT%"=="" set "PORT=8100"

set "FOUND=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
  set "FOUND=1"
  taskkill /F /PID %%a >nul 2>nul
)

if "!FOUND!"=="0" (
  echo No listening process found on port %PORT%
  exit /b 0
)

echo Stopped process listening on port %PORT%
exit /b 0

@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set "ROOT_DIR=%~dp0"
set "FRONTEND_DIR=%ROOT_DIR%core\core-frontend"
set "BACKEND_DIR=%ROOT_DIR%core\core-backend"
set "RELEASE_BASE=%ROOT_DIR%release"
set "DE_PROFILE=%~1"

if "%DE_PROFILE%"=="" set "DE_PROFILE=standalone"
if /I not "%DE_PROFILE%"=="standalone" if /I not "%DE_PROFILE%"=="desktop" if /I not "%DE_PROFILE%"=="distributed" (
  echo Unsupported profile: %DE_PROFILE%
  echo Supported: standalone / desktop / distributed
  exit /b 1
)

for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "[xml]$pom = Get-Content -Raw '%ROOT_DIR%pom.xml'; $pom.project.properties.'dataease.version'"`) do set "DE_VERSION=%%i"
if "%DE_VERSION%"=="" set "DE_VERSION=unknown"

set "RELEASE_NAME=dataease-%DE_VERSION%-windows-%DE_PROFILE%"
set "RELEASE_DIR=%RELEASE_BASE%\%RELEASE_NAME%"
set "ZIP_FILE=%RELEASE_BASE%\%RELEASE_NAME%.zip"

echo ====================================
echo Build frontend
echo ====================================
cd /d "%FRONTEND_DIR%" || exit /b 1
if not exist "node_modules" call npm install || exit /b 1
set "NODE_OPTIONS=--max_old_space_size=4096"
call npx vite build --mode base || exit /b 1
call npm run build:flush || exit /b 1

echo ====================================
echo Build backend
echo ====================================
cd /d "%BACKEND_DIR%" || exit /b 1
call mvn clean package -DskipTests -P %DE_PROFILE% || exit /b 1

if not exist "%BACKEND_DIR%\target\CoreApplication.jar" (
  echo CoreApplication.jar not found
  exit /b 1
)

echo ====================================
echo Assemble release directory
echo ====================================
if not exist "%RELEASE_BASE%" mkdir "%RELEASE_BASE%"
if exist "%RELEASE_DIR%" rmdir /s /q "%RELEASE_DIR%"
mkdir "%RELEASE_DIR%" || exit /b 1
mkdir "%RELEASE_DIR%\app" || exit /b 1
mkdir "%RELEASE_DIR%\conf" || exit /b 1
mkdir "%RELEASE_DIR%\frontend-dist" || exit /b 1
mkdir "%RELEASE_DIR%\logs" || exit /b 1
mkdir "%RELEASE_DIR%\cache" || exit /b 1
mkdir "%RELEASE_DIR%\data" || exit /b 1

copy /y "%BACKEND_DIR%\target\CoreApplication.jar" "%RELEASE_DIR%\app\CoreApplication.jar" >nul || exit /b 1
xcopy "%FRONTEND_DIR%\dist" "%RELEASE_DIR%\frontend-dist\" /e /i /y >nul || exit /b 1
copy /y "%BACKEND_DIR%\src\main\resources\application-standalone.yml" "%RELEASE_DIR%\conf\application-standalone.yml" >nul || exit /b 1
copy /y "%BACKEND_DIR%\src\main\resources\application-desktop.yml" "%RELEASE_DIR%\conf\application-desktop.yml" >nul || exit /b 1
if exist "%BACKEND_DIR%\src\main\resources\application-distributed.yml" (
  copy /y "%BACKEND_DIR%\src\main\resources\application-distributed.yml" "%RELEASE_DIR%\conf\application-distributed.yml" >nul || exit /b 1
)

if exist "%ROOT_DIR%drivers" xcopy "%ROOT_DIR%drivers" "%RELEASE_DIR%\drivers\" /e /i /y >nul
if exist "%ROOT_DIR%custom-drivers" xcopy "%ROOT_DIR%custom-drivers" "%RELEASE_DIR%\custom-drivers\" /e /i /y >nul

copy /y "%ROOT_DIR%scripts\release\start.bat" "%RELEASE_DIR%\start.bat" >nul || exit /b 1
copy /y "%ROOT_DIR%scripts\release\stop.bat" "%RELEASE_DIR%\stop.bat" >nul || exit /b 1

echo ====================================
echo Create zip package
echo ====================================
if exist "%ZIP_FILE%" del /f /q "%ZIP_FILE%"
powershell -NoProfile -Command "Compress-Archive -Path '%RELEASE_DIR%\*' -DestinationPath '%ZIP_FILE%' -Force" || exit /b 1

echo.
echo Release directory: %RELEASE_DIR%
echo Release zip: %ZIP_FILE%
echo.
echo Usage:
echo 1. Unzip package on target machine
echo 2. Backend jar is in app, frontend static is in frontend-dist
echo 3. Double click start.bat
echo 4. Optional profile switch: start.bat desktop
echo.
exit /b 0

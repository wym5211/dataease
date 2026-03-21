@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

set "DE_PROFILE=%~1"
if "%DE_PROFILE%"=="" set "DE_PROFILE=standalone"

set "JAVA_CMD=java"
set "DE_LOCAL_JAVA=%~dp0jre\bin\java.exe"
if exist "%DE_LOCAL_JAVA%" (
  set "JAVA_CMD=%DE_LOCAL_JAVA%"
) else (
  if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)


set "APP_JAR=app\CoreApplication.jar"
if not exist "%APP_JAR%" (
  echo %APP_JAR% not found
  exit /b 1
)

if not exist "logs" mkdir logs
if not exist "cache" mkdir cache
if not exist "data" mkdir data

echo ====================================
echo Start DataEase
echo profile: %DE_PROFILE%
echo java: %JAVA_CMD%
echo ====================================
echo.

"%JAVA_CMD%" -version >nul 2>nul
if errorlevel 1 (
  echo Java 21 is required. Install Java or place runtime under .\jre
  exit /b 1
)

"%JAVA_CMD%" -Dspring.profiles.active=%DE_PROFILE% -Ddataease.path.driver=./drivers -Ddataease.path.custom-drivers=./custom-drivers/ -Ddataease.path.ehcache=./cache -jar "%APP_JAR%" --spring.config.additional-location=optional:file:./conf/
exit /b %ERRORLEVEL%

@echo off
chcp 65001 > nul
echo ========================================
echo Building with UTF-8 Encoding
echo ========================================

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set MAVEN_HOME=E:\apache-maven-3.9.0
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

set MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dproject.encoding=UTF-8 -Dfile.encoding=UTF-8

echo.
echo Building SDK...
cd E:\cursor\dataease\sdk\common
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo SDK build successful!
) else (
    echo SDK build failed with code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Building backend...
cd E:\cursor\dataease\core\core-backend
call mvn package -Dmaven.test.skip=true

if %ERRORLEVEL% EQU 0 (
    echo Backend build successful!
) else (
    echo Backend build failed with code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
pause

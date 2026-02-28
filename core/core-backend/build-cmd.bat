@echo off
echo ========================================
echo Starting Maven Clean Package
echo ========================================
cd /d %~dp0
call mvn clean package -DskipTests
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
) else (
    echo ========================================
    echo BUILD FAILED! Error code: %ERRORLEVEL%
    echo ========================================
)
pause

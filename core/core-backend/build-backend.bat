@echo off
cd /d E:\cursor\dataease\core\core-backend
call mvn clean package -DskipTests
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
) else (
    echo Build failed with error code %ERRORLEVEL%
)

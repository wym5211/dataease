@echo off
REM DataEase 后端开发环境启动脚本

echo ====================================
echo DataEase 后端开发环境启动脚本
echo ====================================

REM 设置Java路径
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

REM 进入后端目录
cd /d "%~dp0core\core-backend"

REM 清理旧的缓存（可选）
if exist "cache" (
    echo 清理缓存目录...
    rmdir /s /q cache 2>nul
)

REM 停止旧进程
echo 停止旧的后端进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8100" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

REM 等待端口释放
timeout /t 3 /nobreak >nul

REM 启动后端（带驱动路径参数）
echo.
echo 启动后端服务...
echo 驱动目录: %~dp0drivers
echo 缓存目录: %~dp0core\core-backend\cache
echo.

java -Dspring.profiles.active=desktop ^
     -Ddataease.path.driver=../../drivers ^
     -Ddataease.path.custom-drivers=../../custom-drivers/ ^
     -Ddataease.path.ehcache=./cache ^
     -jar target/CoreApplication.jar

pause

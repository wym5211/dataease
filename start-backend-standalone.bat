@echo off
REM DataEase 后端 Standalone 模式启动脚本

echo ====================================
echo DataEase 后端 Standalone 模式启动
echo ====================================

REM 设置Java路径
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

REM 进入后端目录
cd /d "%~dp0core\core-backend"

REM 停止旧进程
echo 停止旧的后端进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8100" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

REM 等待端口释放
timeout /t 3 /nobreak >nul

REM 启动后端（standalone模式）
echo.
echo 启动后端服务（standalone模式 - MySQL）...
echo 数据库: jdbc:mysql://localhost:3306/dataease10
echo.

java -Dspring.profiles.active=standalone ^
     -Ddataease.path.driver=../../drivers ^
     -Ddataease.path.custom-drivers=../../custom-drivers/ ^
     -Ddataease.path.ehcache=./cache-standalone ^
     -jar target/CoreApplication.jar

pause

@echo off
REM DataEase 前端开发环境启动脚本

echo ====================================
echo DataEase 前端开发环境启动脚本
echo ====================================

REM 进入前端目录
cd /d "%~dp0core\core-frontend"

REM 检查node_modules是否存在
if not exist "node_modules" (
    echo 首次运行，正在安装依赖...
    call npm install
)

REM 启动前端开发服务器
echo.
echo 启动前端开发服务器...
echo.

npm run dev:win

pause

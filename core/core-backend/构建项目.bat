@echo off
chcp 65001 > nul
echo ========================================
echo    DataEase 后端构建脚本
echo ========================================
echo.

REM 设置Maven路径
set MAVEN_HOME=C:\Program Files\Apache Software Foundation\apache-maven-3.9.2
set PATH=%MAVEN_HOME%\bin;%PATH%

echo 当前目录: %CD%
echo.
echo 开始执行: mvn clean package -DskipTests
echo ========================================
echo.

call mvn clean package -DskipTests

echo.
echo ========================================
if %ERRORLEVEL% EQU 0 (
    echo 构建成功！
    echo.
    echo JAR文件位置: target\CoreApplication.jar
) else (
    echo 构建失败！错误代码: %ERRORLEVEL%
)
echo ========================================
echo.
pause

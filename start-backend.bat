@echo off
REM DataEase 后端启动脚本 (Windows开发环境)

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
set MAVEN_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3

cd /d "%~dp0\core\core-backend"

echo ====================================
echo 启动 DataEase 后端服务...
echo Java版本:
java -version
echo ====================================
echo.

"%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0 -XX:+UseStringDeduplication" -DskipTests

pause

@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
set MAVEN_HOME=E:\apache-maven-3.9.0
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%
set MAVEN_OPTS=-Dfile.encoding=UTF-8

cd E:\cursor\dataease\sdk\common
call mvn clean package -DskipTests
cd ..\..\core\core-backend
call mvn package -Dmaven.test.skip=true
pause

@echo off
setlocal
cd /d "%~dp0"
if not exist ".runtime" mkdir ".runtime"
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" "-Djdk.net.unixdomain.tmpdir=%~dp0.runtime" -jar "executavel\produtos-soap.jar"
) else (
    java "-Djdk.net.unixdomain.tmpdir=%~dp0.runtime" -jar "executavel\produtos-soap.jar"
)
if errorlevel 1 (
    echo.
    echo Verifique se o Java 17 ou superior esta instalado e se a porta 8080 esta livre.
    pause
)

@echo off
setlocal
cd /d "%~dp0"
if not defined SOAP_SECURITY_USERNAME (
    echo Defina SOAP_SECURITY_USERNAME antes de iniciar o servico.
    pause
    exit /b 1
)
if not defined SOAP_SECURITY_PASSWORD (
    echo Defina SOAP_SECURITY_PASSWORD antes de iniciar o servico.
    pause
    exit /b 1
)
if not exist ".runtime" mkdir ".runtime"
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" "-Djdk.net.unixdomain.tmpdir=%~dp0.runtime" -jar "executavel\produtos-soap.jar"
) else (
    java "-Djdk.net.unixdomain.tmpdir=%~dp0.runtime" -jar "executavel\produtos-soap.jar"
)
if errorlevel 1 (
    echo.
    echo Verifique as credenciais, o Java 17 ou superior e se a porta 8080 esta livre.
    pause
)

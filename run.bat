@echo off
setlocal

echo =====================================================================
echo  CodeAlpha - Advanced Hotel Reservation and Management System
echo =====================================================================
echo.

:: Automatically free port 8080 if an earlier instance is still running
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo [INFO] Freeing port 8080 (stopping PID %%a)...
    taskkill /f /pid %%a >nul 2>&1
)

if exist "C:\Users\KEERTHANA\apache-maven-3.9.9\bin\mvn.cmd" (
    echo [INFO] Found Maven at C:\Users\KEERTHANA\apache-maven-3.9.9. Starting Spring Boot application...
    "C:\Users\KEERTHANA\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
    goto end
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Found Maven in PATH. Starting Spring Boot application...
    mvn spring-boot:run
    goto end
)

if exist mvnw.cmd (
    echo [INFO] Using Maven Wrapper...
    call mvnw.cmd spring-boot:run
    goto end
)

echo [WARNING] Maven is not detected in your system PATH.
echo Please run this project in your preferred IDE (IntelliJ IDEA, Eclipse, VS Code)
echo.

:end
pause

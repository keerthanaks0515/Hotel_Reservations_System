@echo off
setlocal

echo =====================================================================
echo  CodeAlpha - Running Automated Test Suite (JUnit & Mockito)
echo =====================================================================
echo.

if exist "C:\Users\KEERTHANA\apache-maven-3.9.9\bin\mvn.cmd" (
    echo [INFO] Running test suite via installed Maven...
    "C:\Users\KEERTHANA\apache-maven-3.9.9\bin\mvn.cmd" test
    goto end
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Running test suite...
    mvn test
    goto end
)

if exist mvnw.cmd (
    call mvnw.cmd test
    goto end
)

:end
pause

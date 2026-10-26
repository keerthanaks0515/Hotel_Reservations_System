@echo off
setlocal
echo Freeing port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Stopping PID %%a on port 8080...
    taskkill /f /pid %%a
)
echo Port 8080 is now free.
pause

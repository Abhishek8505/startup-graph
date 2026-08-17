@echo off
setlocal
cd /d "%~dp0"

if not exist ".env" (
    echo ERROR: .env not found. Copy .env.example to .env and fill in COGNODB_URI / COGNODB_PASSWORD.
    pause
    exit /b 1
)

if not exist "target\startup-graph-1.0.0.jar" (
    echo Building the application (one-time, a few minutes on first run)...
    call mvnw.cmd -q -DskipTests package
    if errorlevel 1 (
        echo Build failed. See the output above.
        pause
        exit /b 1
    )
)

set PORT=8080
for /f "usebackq tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"

set "JAVA_HOME="
for /d %%d in ("%USERPROFILE%\devtools\jdk-*") do set "JAVA_HOME=%%d"
set "JAVA_CMD=java"
if defined JAVA_HOME set "JAVA_CMD=%JAVA_HOME%\bin\java"

echo.
echo VentureGraph is starting at  http://localhost:8080
echo The browser will open automatically. Press Ctrl+C to stop.
echo.
start "" cmd /c "timeout /t 12 /nobreak >nul & start http://localhost:8080"
"%JAVA_CMD%" -jar target\startup-graph-1.0.0.jar
pause

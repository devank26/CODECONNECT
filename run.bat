@echo off
setlocal

:: ── Configuration ─────────────────────────────────────────────────────────────
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN_VERSION=3.9.6
set MVN_DIR=%~dp0.mvn-local
set MVN_ZIP=%MVN_DIR%\apache-maven-%MVN_VERSION%-bin.zip
set MVN_HOME=%MVN_DIR%\apache-maven-%MVN_VERSION%
set MVN_CMD=%MVN_HOME%\bin\mvn.cmd
set PROJECT_DIR=%~dp0

echo ===============================================
echo  Java Platform — Build and Run
echo ===============================================
echo  JAVA_HOME: %JAVA_HOME%
echo.

:: ── Download Maven if not already present ────────────────────────────────────
if not exist "%MVN_CMD%" (
    echo [1/3] Downloading Apache Maven %MVN_VERSION%...
    if not exist "%MVN_DIR%" mkdir "%MVN_DIR%"

    powershell -Command ^
        "[Net.ServicePointManager]::SecurityProtocol = 'Tls12'; " ^
        "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MVN_VERSION%/binaries/apache-maven-%MVN_VERSION%-bin.zip' " ^
        "-OutFile '%MVN_ZIP%'"

    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to download Maven. Check your internet connection.
        pause
        exit /b 1
    )

    echo [2/3] Extracting Maven...
    powershell -Command "Expand-Archive -Path '%MVN_ZIP%' -DestinationPath '%MVN_DIR%' -Force"
    del "%MVN_ZIP%"
    echo Maven ready at: %MVN_HOME%
) else (
    echo [1/3] Maven already downloaded, skipping.
)

:: ── Build and run ─────────────────────────────────────────────────────────────
echo [3/3] Building and launching Java Platform...
echo.
cd /d "%PROJECT_DIR%"
"%MVN_CMD%" clean javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED. Check the output above for errors.
    pause
)
endlocal

@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "LIB_DIR=lib"
set "SRC_DIR=src\main\java"
set "OUT_DIR=target\classes"
set "MAIN_CLASS=com.bms.ui.AppFrame"

:: Build classpath from lib/*.jar
set "CP="
for %%j in ("%LIB_DIR%\*.jar") do (
    if defined CP (
        set "CP=!CP!;%%~fj"
    ) else (
        set "CP=%%~fj"
    )
)
set "CP=%CP%;%OUT_DIR%"

:: Compile if output directory does not exist or sources are newer
echo Compiling project...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

:: Collect all Java source files
set "SRC_FILE=%OUT_DIR%\sources.txt"
if exist "%SRC_FILE%" del "%SRC_FILE%"
for /r "%SRC_DIR%" %%f in (*.java) do (
    echo %%~ff >> "%SRC_FILE%"
)

javac --release 17 -encoding UTF-8 -cp "%CP%" -d "%OUT_DIR%" @"%SRC_FILE%"
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo Running %MAIN_CLASS%...
java -cp "%CP%" %MAIN_CLASS%

endlocal

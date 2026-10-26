
@echo off
cd /d "%~dp0"
setlocal

if not exist out mkdir out
if not exist data mkdir data

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = Get-ChildItem -Path 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }; if (-not $files) { Write-Error 'No Java source files found.'; exit 1 }; & javac -encoding UTF-8 -d 'out' $files; exit $LASTEXITCODE"

if errorlevel 1 (
    echo.
    echo Build failed.
    pause
    exit /b 1
)

java -Dsilo.data=data -cp out com.silo.ui.SiloApp

pause

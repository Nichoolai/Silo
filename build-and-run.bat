@echo off
cd /d "%~dp0"
setlocal
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven is not installed or not on PATH. Install Maven or run from VS Code using the Java Extension Pack.
  pause
  exit /b 1
)
mvn -q -DskipTests package dependency:copy-dependencies
if errorlevel 1 (
  echo Build failed. Check Java 21 and Maven setup.
  pause
  exit /b 1
)
if not exist data mkdir data
java -Dsilo.data=data -cp "target/classes;target/dependency/*" com.silo.ui.SiloApp
pause

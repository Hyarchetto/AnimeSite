@echo off
rem Launcher for start-mysql.ps1, so double-click and any terminal can start the 3307 instance
rem Kept plain ASCII on purpose - cmd.exe misparses batch files containing non-ASCII characters
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-mysql.ps1" %*
exit /b %errorlevel%

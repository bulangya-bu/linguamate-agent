@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop.ps1" %*
set "LM_EXIT=%ERRORLEVEL%"
if not "%LM_EXIT%"=="0" pause
exit /b %LM_EXIT%

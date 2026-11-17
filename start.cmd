@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" %*
set "LM_EXIT=%ERRORLEVEL%"
if not "%LM_EXIT%"=="0" pause
if "%LM_EXIT%"=="0" if "%~1"=="" (
    setlocal EnableDelayedExpansion
    if /i not "!CMDCMDLINE: /c =!"=="!CMDCMDLINE!" pause
)
exit /b %LM_EXIT%

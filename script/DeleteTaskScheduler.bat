@echo off
set SLEEP_TME=5
Schtasks /delete /tn "StartHarvester" /f
if %ERRORLEVEL% NEQ 0 (
	echo Windows scheduled task "StartHarvester" might have been deleted already before.
)
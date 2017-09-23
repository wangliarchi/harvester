@echo off
Schtasks /create /sc DAILY /mo 1 /st 02:00 /tn "StartHarvester" /tr C:\Harvester\app-data\tools\StartApplication.bat /f
set SLEEP_TME=5
echo =============================================================
echo Harvester will be started everyday day at 2:00 AM if it was closed by mistakes.
echo However it only works when Harvester was installed properly at C:\Harvester.
echo Meanwhile, please ensure you have configured Harvester properly.
echo This dialog box will be closed after %SLEEP_TME% seconds.
echo =============================================================
timeout %SLEEP_TME%
@echo off
Schtasks /create /sc minute /mo 240 /tn "StartHarvester" /tr C:\Harvester\app-data\tools\StartApplication.bat /f
set SLEEP_TME=5
echo =============================================================
echo This task will check if Harvester is running every 4 hours, and start the program if it's stopped.
echo However it only works when Harvester was installed properly at C:\Harvester.
echo Meanwhile, please ensure you have configured Harvester properly.
echo This dialog box will be closed after %SLEEP_TME% seconds.
echo =============================================================
timeout %SLEEP_TME%
@echo off
rem =============================================================
rem Harvester will be started everyday day at 2:00 AM if it was closed by mistakes.
rem However it only works when Harvester was installed properly at C:\Harvester.
rem Meanwhile, please ensure you have configured Harvester properly.
rem This dialog box will be closed after %SLEEP_TME% seconds.
rem =============================================================
Schtasks /create /sc DAILY /mo 1 /st 02:00 /tn "StartHarvester" /tr C:\Harvester\app-data\tools\StartApplication.bat /f
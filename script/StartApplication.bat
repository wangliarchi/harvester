@echo off
set SLEEP_TME=5
if not exist C:\Harvester\Harvester.exe (
	echo It seems that Harvester was not installed properly. Please fix this error first.
	exit
)
cd /d C:\Harvester
rem It may also be enhanced to check whether Harvester is running or not first using wmic
if exist "app-data\customize\harvester-config.json" (
	start Harvester.exe
	echo Harvester was started successfully.
	echo This dialog box will be closed after %SLEEP_TME% seconds.
) else (
	echo Harvester configuration file missing, it is meaningless to start it.
)
timeout %SLEEP_TME%
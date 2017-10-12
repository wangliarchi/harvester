@echo off
if not exist C:\Harvester\Harvester.exe (
  echo It seems that Harvester was not installed properly. Please fix this error first.
  exit
)
cd /d C:\Harvester
if not exist "app-data\customize\harvester-config.json" (
  echo Harvester configuration file missing, it is meaningless to start it.
  exit
)

wmic process list brief | find /i "Harvester.exe"
if %ERRORLEVEL% == 0 (
  echo Harvester is running. There is no need to start it again.
) else (
  echo No running process of Harvester was found and we will start it directly.
  cd /d C:\Harvester
  start Harvester.exe
  echo Harvester was started successfully.
)
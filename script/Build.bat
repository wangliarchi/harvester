@echo off

title Arguments Validation
set argCount=0
for %%x in (%*) do (
   set /A argCount+=1
)
if not %argCount% == 3 (
	echo ***************************************
	echo Arguments are illegal.
	echo You can and must provide 3 arguments:
	echo The first argument can be: INST, BUILD
	echo The second argument can be: GA, BETA
	echo The third argument can be: FULL, DELTA
	echo ***************************************
	pause
	exit
) else (
    set BEGIN_TIME=%TIME%
	echo Scenario: %1, %2, %3
)

title Initialize Environment Paths
set "SCRIPT_DIR=%cd%"
echo Scripts directory: %SCRIPT_DIR%

cd ..\
if not defined ROOT_DIR set "ROOT_DIR=C:\OURnd"
echo Root directory: %ROOT_DIR%

if not defined PROJECT_DIR set "PROJECT_DIR=%cd%"
echo Harvester project directory: %PROJECT_DIR%

if not defined JAVA_HOME set JAVA_HOME=%ROOT_DIR%\Software\jdk
if not defined MAVEN_HOME set MAVEN_HOME=%ROOT_DIR%\Software\apache-maven-3.5.0
echo JAVA_HOME: %JAVA_HOME%, MAVEN_HOME: %MAVEN_HOME%

if not defined DEST_DIR set DEST_DIR=C:\Harvester

if not defined DROPBOX_DIR set DROPBOX_DIR=%USERPROFILE%\Dropbox
if not exist %DROPBOX_DIR% set /p DROPBOX_DIR=Enter Dropbox Directory:
:: BETA mode or GA mode?
if [%2] == [GA] (set PREFIX=\) else (set PREFIX=\%2%\)
set DIST_DIR=%DROPBOX_DIR%%PREFIX%Harvester
echo Distribution directory: %DIST_DIR%
echo *********************************************************

set VERSION=1.0
set PATH=%PATH%;%JAVA_HOME%\bin;%MAVEN_HOME%\bin;
set BUILD_OPS=-Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dcheckstyle.skip=true
if [%1] == [BUILD] (
	if [%3] == [FULL] goto fullyBuild
	if [%3] == [DELTA] goto deltaBuild
)

:deltaBuild
title Build Project in Delta Mode
:: Delta模式构建
cd /d %PROJECT_DIR%
call mvn.bat install %BUILD_OPS%
if %ERRORLEVEL% NEQ 0 (
    echo Failed to build Harvester project. Please check and correct it first.
    pause
    exit
)

copy %PROJECT_DIR%\target\harvester-%VERSION%.jar	%DIST_DIR%\lib\harvester.jar /y
echo Harvester jar file has been copied to Dropbox folder successfully
goto ending

:fullyBuild
title Fully Build Harvester Project

:buildHarvester
cd /d %PROJECT_DIR%
set CP=%PROJECT_DIR%\target\*;%PROJECT_DIR%\target\deps\*
if not exist %PROJECT_DIR%\app-data\tmp mkdir %PROJECT_DIR%\app-data\tmp
java -cp %CP% edu.olivet.foundations.utils.CurrencyRateCalculatorImpl
java -cp %CP% edu.olivet.foundations.release.ReleasePublisher Harvester
echo Harvester documentation generated successfully.

cd /d %PROJECT_DIR%
call mvn.bat clean install %BUILD_OPS%
if %ERRORLEVEL% NEQ 0 (
    echo Failed to clean and install Harvester project. Please check and correct it first.
    pause
    exit
)

:sync2Dropbox
copy %PROJECT_DIR%\target\harvester-%VERSION%.jar  		%DIST_DIR%\lib\harvester.jar /y
xcopy %PROJECT_DIR%\target\deps\*.jar 	    			%DIST_DIR%\lib\deps\ /d /y /q
xcopy %PROJECT_DIR%\src\main\resources\logback*.xml		%DIST_DIR%\app-data\customize\ /d /y /q
xcopy %PROJECT_DIR%\app-data\customize\currency-rates	%DIST_DIR%\app-data\customize\ /d /y /q
xcopy %PROJECT_DIR%\template\*.* 		  				%DIST_DIR%\template\ /d /y /e /q
echo Programming files of Harvester V%VERSION% synchronized to Dropbox successfully.

copy %PROJECT_DIR%\target\harvester-%VERSION%.jar  		C:\Harvester\lib\harvester.jar /y
xcopy %PROJECT_DIR%\target\deps\*.jar 	    			C:\Harvester\lib\deps\ /d /y /q
xcopy %PROJECT_DIR%\src\main\resources\logback*.xml		C:\Harvester\app-data\customize\ /d /y /q
xcopy %PROJECT_DIR%\app-data\customize\currency-rates	C:\Harvester\app-data\customize\ /d /y /q
xcopy %PROJECT_DIR%\template\*.* 		  				C:\Harvester\template\ /d /y /e /q
echo Programming files of Harvester V%VERSION% copied to C:\Harvester successfully.

:updateVersion
java -cp %CP% edu.olivet.foundations.release.VersionManager Harvester %DIST_DIR%

:ending
java -cp %PROJECT_DIR%\target\deps\* edu.olivet.deploy.Md5Calculator %DIST_DIR%
echo **************** Began at %BEGIN_TIME%, finished at %TIME% *************
cd /d %SCRIPT_DIR%
timeout 7
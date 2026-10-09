@echo off
REM SmartCityX launcher for Windows: compiles the sources, then starts the program.
REM
REM   run.bat              interactive menu
REM   run.bat --all        run all nine demonstrations with the sample inputs
REM   run.bat --demo 4     run one demonstration (1-9)
REM   run.bat --test       run the automated tests
REM   run.bat --data       print the sample city datasets
REM   run.bat --help       list every option
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo SmartCityX needs a JDK ^(Java 11 or newer^), but javac was not found.
    echo Install one from https://adoptium.net and open a new terminal.
    exit /b 1
)

REM Every source file is listed, so the build never depends on old .class files.
javac -d out -sourcepath . Main.java algorithms\*.java data\*.java tests\*.java ui\*.java
if errorlevel 1 (
    echo Compilation failed. See the messages above.
    exit /b 1
)

java -cp out Main %*

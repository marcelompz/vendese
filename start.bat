@echo off
REM    uniCenta oPOS - Touch Friendly Point of Sale
REM    Copyright (c) 2009-2014 uniCenta
REM    http://sourceforge.net/projects/unicentaopos
REM
REM    This file is part of uniCenta oPOS
REM
REM    uniCenta oPOS is free software: you can redistribute it and/or modify
REM    it under the terms of the GNU General Public License as published by
REM    the Free Software Foundation, either version 3 of the License, or
REM    (at your option) any later version.
REM
REM    uniCenta oPOS is distributed in the hope that it will be useful,
REM    but WITHOUT ANY WARRANTY; without even the implied warranty of
REM    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM    GNU General Public License for more details.
REM
REM    You should have received a copy of the GNU General Public License
REM    along with uniCenta oPOS.  If not, see http://www.gnu.org/licenses/>

setlocal
pushd "%~dp0" >nul

REM --- Locate Java 8+ (8/17 LTS): JAVA_HOME, PATH, C:\tools\jdk*, common folders ---
set "JAVAX="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAX=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVAX for %%J in ("javaw.exe") do set "JAVAX=%%~$PATH:J"
if not defined JAVAX (
    for /d %%D in ("C:\tools\jdk*") do if exist "%%D\bin\javaw.exe" set "JAVAX=%%D\bin\javaw.exe"
)
if not defined JAVAX (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do if exist "%%D\bin\javaw.exe" set "JAVAX=%%D\bin\javaw.exe"
)
if not defined JAVAX (
    for /d %%D in ("%ProgramFiles(x86)%\Eclipse Adoptium\jdk-*") do if exist "%%D\bin\javaw.exe" set "JAVAX=%%D\bin\javaw.exe"
)
if not defined JAVAX (
    echo.
    echo No se encontro Java 8 o superior ^(javaw.exe^).
    echo Instale un JRE/JDK 8 o 17 LTS, o defina la variable JAVA_HOME.
    echo.
    pause
    popd
    endlocal
    exit /b 1
)

REM lib\* expands to every JAR in lib (Java 6+); locales and reports are resources.
set "CP=unicentaopos.jar;lib\*;locales;reports"

REM JDK 9+ needs explicit opens for legacy Swing/Substance/Jasper reflection.
set "JAVA9_OPTS="
for %%J in ("%JAVAX%") do set "JAVA_TEST=%%~dpJjava.exe"
if exist "%JAVA_TEST%" (
    "%JAVA_TEST%" --add-opens java.base/java.lang=ALL-UNNAMED -version >nul 2>&1
    if not errorlevel 1 set "JAVA9_OPTS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED"
)

REM Bytecode remains Java 7 (source/target 1.7); runtime may be 8 or 17 LTS.
start "" "%JAVAX%" %JAVA9_OPTS% -cp "%CP%" -Djava.library.path="lib\Windows\i368-mingw32" -Ddirname.path=./ -splash:unicenta_splash_dark.png com.openbravo.pos.forms.StartPOS %*

popd
endlocal

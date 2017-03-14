@REM Copyright (c) Microsoft. All rights reserved.
@REM Licensed under the MIT license. See LICENSE file in the project root for full license information.

@setlocal EnableExtensions EnableDelayedExpansion
@echo off

set current-path=%~dp0

rem // remove trailing slash
set current-path=%current-path:~0,-1%

set build-root=%current-path%\..
rem // resolve to fully qualified path
for %%i in ("%build-root%") do set build-root=%%~fi

REM set local-install=%build-root%\install-deps

REM set build-jnano-root=%current-path%\..\build_jnano

REM echo Cleaning up build artifacts...

rem Clear the jnano build folder so we have a fresh build
REM rmdir /s/q %build-jnano-root%
REM if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

REM mkdir %build-jnano-root%
REM if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

REM pushd %build-jnano-root%

REM rem Clone jnano
REM git clone -b develop https://github.com/ancaantochi/jnano.git

REM popd

REM pushd %build-jnano-root%\jnano
REM call mvn clean install -DskipTests
REM if errorlevel 1 goto :eof
REM popd

REM -- Build Java Remote SDK --
pushd %build-root%\proxy\gateway\java\gateway-remote-module
call mvn clean install
if errorlevel 1 goto :eof
popd %build-root%

:eof
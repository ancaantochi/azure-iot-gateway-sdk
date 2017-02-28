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

set local-install=%build-root%\install-deps

set build-jnano-root=%current-path%\..\build_jnano

echo Cleaning up build artifacts...

rem Clear the jnano build folder so we have a fresh build
rmdir /s/q %build-jnano-root%
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

mkdir %build-jnano-root%
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

pushd %build-jnano-root%

rem Clone jnano
git clone -b develop https://github.com/ancaantochi/jnano.git

popd

pushd %build-jnano-root%\jnano
call mvn clean install -DskipTests
if errorlevel 1 goto :eof
popd

REM -- Build Java Remote SDK --
pushd %build-root%\proxy\gateway\java\gateway-remote-module-sdk
call mvn clean install
if errorlevel 1 goto :eof
popd %build-root%

:eof
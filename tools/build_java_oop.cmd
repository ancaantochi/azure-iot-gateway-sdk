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

rem // default build options
if "%build-config%" == "" set build-config=Debug
if "%build-platform%" == "" set build-platform=Win32
if "%use_xplat_uuid%" == "" set use_xplat_uuid=OFF
if %dependency_install_prefix% == "" set dependency_install_prefix="-Ddependency_install_prefix=%local-install%"

rem -----------------------------------------------------------------------------
rem -- build with CMAKE
rem -----------------------------------------------------------------------------

set build-root=%current-path%\..\build_jnano
set "cmake-root=%build-root%\jnano"

echo Cleaning up build artifacts...

rem Clear the jnano build folder so we have a fresh build
rmdir /s/q %build-root%
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

mkdir %build-root%
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

pushd %build-root%

rem Clone jnano
git clone -b develop https://github.com/ancaantochi/jnano.git

popd

pushd %cmake-root%

if %build-platform% == x64 (
    echo ***Running CMAKE for Win64***
        cmake %dependency_install_prefix% -DCMAKE_BUILD_TYPE="%build-config%" -Duse_xplat_uuid:BOOL=%use_xplat_uuid% -G "Visual Studio 14 Win64" "%cmake-root%"
        if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!
) else (
    echo ***Running CMAKE for Win32***
        cmake %dependency_install_prefix% -DCMAKE_BUILD_TYPE="%build-config%" -Duse_xplat_uuid:BOOL=%use_xplat_uuid% -G "Visual Studio 14" "%cmake-root%"
        if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!
)

msbuild /m /p:Configuration="%build-config%" /p:Platform="%build-platform%" Project.sln
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

call mvn clean install -DskipTests
if errorlevel 1 goto :eof

popd
goto :eof


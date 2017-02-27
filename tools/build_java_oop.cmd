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

rem ----------------------------------------------------------------------------
rem -- parse script arguments
rem ----------------------------------------------------------------------------

rem // default build options
set build-config=Debug
set build-platform=Win32
set use_xplat_uuid=OFF
set dependency_install_prefix="-Ddependency_install_prefix=%local-install%"

:args-loop
if "%1" equ "" goto args-done
if "%1" equ "--config" goto arg-build-config
if "%1" equ "--platform" goto arg-build-platform
if "%1" equ "--system-deps-path" goto arg-system-deps-path
if "%1" equ "--use-xplat-uuid" goto arg-use-xplat-uuid

call :usage && exit /b 1

:arg-build-config
shift
if "%1" equ "" call :usage && exit /b 1
set build-config=%1
goto args-continue

:arg-build-platform
shift
if "%1" equ "" call :usage && exit /b 1
set build-platform=%1
goto args-continue

:arg-system-deps-path
set dependency_install_prefix=""
goto args-continue

:arg-use-xplat-uuid
set use_xplat_uuid=ON
goto args-continue

:args-continue
shift
goto args-loop

:args-done

rem -----------------------------------------------------------------------------
rem -- build with CMAKE and run tests
rem -----------------------------------------------------------------------------

rem this is setting the cmake path in a quoted way

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
        cmake -DCMAKE_BUILD_TYPE="%build-config%" -Duse_xplat_uuid:BOOL=%use_xplat_uuid% -G "Visual Studio 14 Win64" "%cmake-root%"
        if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!
) else (
    echo ***Running CMAKE for Win32***
        cmake -DCMAKE_BUILD_TYPE="%build-config%" -Duse_xplat_uuid:BOOL=%use_xplat_uuid% -G "Visual Studio 14" "%cmake-root%"
        if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!
)

msbuild /m /p:Configuration="%build-config%" /p:Platform="%build-platform%" Project.sln
if not !ERRORLEVEL!==0 exit /b !ERRORLEVEL!

call mvn clean install -DskipTests
if errorlevel 1 goto :eof

popd
goto :eof

rem -----------------------------------------------------------------------------
rem -- subroutines
rem -----------------------------------------------------------------------------

:usage
echo build.cmd [options]
echo options:
echo  --config value            Build configuration (e.g. [Debug], Release)
echo  --platform value          Build platform (e.g. [Win32], x64, ...)
echo  --system-deps-path        Search for dependencies in a system-level location,
echo                            e.g. "C:\Program Files (x86)", and install if not
echo                            found. When this option is omitted the path is
echo                            %local-install%.
echo  --use-xplat-uuid          Use SDK's platform-independent UUID implementation
goto :eof


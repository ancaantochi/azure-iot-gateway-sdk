#!/bin/bash
# Copyright (c) Microsoft. All rights reserved.
# Licensed under the MIT license. See LICENSE file in the project root for full license information.

set -e

build_root=$(cd "$(dirname "$0")/.." && pwd)
build_root=$build_root/build_jnano

local_install=$build_root/install-deps
log_dir=$build_root
CORES=0

if [ -z $dependency_install_prefix ]
then
    dependency_install_prefix="-Ddependency_install_prefix=$local_install"
fi
if [ -z $build_config ]
then
    build_config=Debug
fi
if [ -z $use_xplat_uuid ]
then
    use_xplat_uuid=OFF
fi

echo $build_config

get_cores ()
{
    CORES=$(grep -c ^processor /proc/cpuinfo 2>/dev/null || sysctl -n hw.ncpu)

    # Make sure there is enough virtual memory on the device to handle more than one job.
    # We arbitrarily decide that 500 MB per core is what we need in order to run the build
    # in parallel.
    MINVSPACE=$(expr 500000 \* $CORES)

    # Acquire total memory and total swap space setting them to zero in the event the command fails
    MEMAR=( $(sed -n -e 's/^MemTotal:[^0-9]*\([0-9][0-9]*\).*/\1/p' -e 's/^SwapTotal:[^0-9]*\([0-9][0-9]*\).*/\1/p' /proc/meminfo) )
    [ -z "${MEMAR[0]##*[!0-9]*}" ] && MEMAR[0]=0
    [ -z "${MEMAR[1]##*[!0-9]*}" ] && MEMAR[1]=0

    let VSPACE=${MEMAR[0]}+${MEMAR[1]}

    if [ "$VSPACE" -lt "$MINVSPACE" ] ; then
    # We think the number of cores to use is a function of available memory divided by 500 MB
    CORES2=$(expr ${MEMAR[0]} / 500000)

    # Clamp the cores to use to be between 1 and $CORES (inclusive)
    CORES2=$([ $CORES2 -le 0 ] && echo 1 || echo $CORES2)
    CORES=$([ $CORES -le $CORES2 ] && echo $CORES || echo $CORES2)
    fi
}

get_cores

# clear the jnano build folder so we have a fresh build
rm -rf $build_root
mkdir -p $build_root

# build jnano
pushd $build_root
git clone -b develop https://github.com/ancaantochi/jnano.git
popd

cmake_root="$build_root/jnano"

pushd "$cmake_root"

cmake $toolchainfile \
      $dependency_install_prefix \
      -DcompileOption_C:STRING="$extracloptions" \
      -DCMAKE_BUILD_TYPE="$build_config" \
      -Dbuild_cores=$CORES \
      -Duse_xplat_uuid:BOOL=$use_xplat_uuid \
      "$cmake_root"

make --jobs=$CORES

mvn clean install -DskipTests
[ $? -eq 0 ] || exit $?

popd

[ $? -eq 0 ] || exit $?


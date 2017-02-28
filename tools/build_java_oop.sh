#!/bin/bash
# Copyright (c) Microsoft. All rights reserved.
# Licensed under the MIT license. See LICENSE file in the project root for full license information.

set -e

build_root=$(cd "$(dirname "$0")/.." && pwd)
build_root=$build_root/build_jnano

# clear the jnano build folder so we have a fresh build
rm -rf $build_root
mkdir -p $build_root

# build jnano
pushd $build_root
git clone -b develop https://github.com/ancaantochi/jnano.git
popd

pushd "$build_root/jnano"
mvn clean install -DskipTests
[ $? -eq 0 ] || exit $?

popd

[ $? -eq 0 ] || exit $?


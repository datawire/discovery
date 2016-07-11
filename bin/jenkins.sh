#!/usr/bin/env bash
set -euxo pipefail

# Copyright 2015, 2016 Datawire. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

BIN_PATH="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
source ${BIN_PATH}/common.sh

# Command line option parsing

FLAG_DEBIAN=no
FLAG_DOCKER=no

for i in "$@"; do
case "$i" in
#    -a=*|--arg=*)
#    ARG="${i#*=}"
#    ;;
    --deb)
    FLAG_DEBIAN=yes
    ;;
    --docker)
    FLAG_DOCKER=yes
    ;;
    *)

    ;;
esac
done

printf "$FLAG_DEBIAN"

# Update this to indicate what programs are required before the script can successfully run.
REQUIRED_PROGRAMS=(fpm deb-s3)

WORKSPACE_DIR="${WORKSPACE:?Jenkins \$WORKSPACE environment variable is not set}"
BUILD_TOOLS_DIR="${WORKSPACE_DIR}/build-tools"

QUARK_INSTALL_URL="https://raw.githubusercontent.com/datawire/quark/master/install.sh"
QUARK_BRANCH="v1.0.319"
QUARK_INSTALL_DIR="${BUILD_TOOLS_DIR}/quark"
QUARK_INSTALL_ARGS="-qqq -t ${QUARK_INSTALL_DIR} ${QUARK_BRANCH}"
QUARK_EXEC="${QUARK_INSTALL_DIR}/bin/quark"

VIRTUALENV="${BUILD_TOOLS_DIR}/virtualenv"

sanity_check "${REQUIRED_PROGRAMS[@]}"
mkdir -p ${BUILD_TOOLS_DIR}

header "Setup Python virtualenv"
set +u
virtualenv ${VIRTUALENV}
. ${VIRTUALENV}/bin/activate
set -u

if ! command -v quark >/dev/null 2>&1; then
    # TODO(FEATURE, Quark Installer):
    # The Quark installer should be modified so the $PATH test can be disabled if installing to a specific location.

    header "Setup Datawire Quark"
    curl -sL "$QUARK_INSTALL_URL" | bash -s -- ${QUARK_INSTALL_ARGS}
    . ${QUARK_INSTALL_DIR}/config.sh
    quark --version
fi

header "Build JAR"
./gradlew clean build :discovery-web:shadowJar

header "Build OS packages and Docker images"

if [[ "$FLAG_DEBIAN" = "yes" ]]; then
    bin/build-deb.sh
fi

header "Publishing packages and images"
deb-s3 upload --bucket d6e-debian-pkg-repository --arch amd64 --codename xenial --preserve-versions true build/distributions/deb/*.deb
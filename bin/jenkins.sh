#!/usr/bin/env bash

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
set -euo pipefail

BUILD_ROOT="${WORKSPACE:?Jenkins WORKSPACE environment variable is not set}/build"

QUARK_INSTALL_URL="https://raw.githubusercontent.com/datawire/quark/master/install.sh"
QUARK_INSTALL_ARGS="-qqq -t ${BUILD_ROOT}/quark"
QUARK_BRANCH="master"
QUARK_ROOT="${BUILD_ROOT}/quark"
QUARK_EXEC="${QUARK_ROOT}/bin/quark"

VIRTUALENV="${BUILD_ROOT}/virtualenv"

msg() {
    printf "%s\n" "--> ${1:?Message content not set}"
}

msg "Create and configure virtualenv"
set +u
virtualenv ${VIRTUALENV}
. ${VIRTUALENV}/bin/activate
set -u

if ! command -v quark >/dev/null 2>&1; then
    # TODO(FEATURE, Quark Installer):
    # The Quark installer should be modified so the $PATH test can be disabled if installing to a specific location.

    msg "Install Datawire Quark"
    curl -sL "$QUARK_INSTALL_URL" | bash -s -- ${QUARK_INSTALL_ARGS} ${QUARK_BRANCH}
    . ${BUILD_ROOT}/quark/config.sh
fi

./gradlew clean build :discovery-web:shadowJar
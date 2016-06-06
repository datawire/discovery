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
set -x

BIN_PATH="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
source ${BIN_PATH}/common.sh

PACKAGE_DEPENDENCIES="$(read_prop deb_dependencies)"
PACKAGE_VERSION=2.0.0

DIST_ASSEMBLY_DIR=build/distributions/deb

# Copy the build output artifact into the assembly directory.
mkdir -p ${DIST_ASSEMBLY_DIR}/opt/${PACKAGE_NAME}
cp ${ROOT_PROJECT_NAME}-web/build/libs/${ROOT_PROJECT_NAME}-web-${PACKAGE_VERSION}-fat.jar ${DIST_ASSEMBLY_DIR}/opt/${PACKAGE_NAME}/

# The pkg-bin directory is where the various before/after install/uninstall scripts for a Debian package are stored.
mkdir -p ${DIST_ASSEMBLY_DIR}/pkg-bin
cp ${PACKAGE_PROJECT_MODULE}/dist/pkg-bin/* ${DIST_ASSEMBLY_DIR}/pkg-bin
chmod +x ${DIST_ASSEMBLY_DIR}/pkg-bin/*

# Configuration files
mkdir -p ${DIST_ASSEMBLY_DIR}/etc/${ROOT_PROJECT_NAME}
mkdir -p ${DIST_ASSEMBLY_DIR}/etc/nginx/sites-available
mkdir -p ${DIST_ASSEMBLY_DIR}/var/awslogs/etc/config

cp ${PACKAGE_PROJECT_MODULE}/config/${ROOT_PROJECT_NAME}.json          ${DIST_ASSEMBLY_DIR}/etc/${ROOT_PROJECT_NAME}/
cp ${PACKAGE_PROJECT_MODULE}/dist/systemd-${ROOT_PROJECT_NAME}.service ${DIST_ASSEMBLY_DIR}/${PACKAGE_NAME}.service
cp ${PACKAGE_PROJECT_MODULE}/dist/nginx-${ROOT_PROJECT_NAME}.conf      ${DIST_ASSEMBLY_DIR}/etc/nginx/sites-available/${PACKAGE_NAME}.conf
cp ${PACKAGE_PROJECT_MODULE}/dist/cloudwatch-${ROOT_PROJECT_NAME}.conf ${DIST_ASSEMBLY_DIR}/var/awslogs/etc/config/${PACKAGE_NAME}.conf

# Run FPM in the distribution assembly directory (-C)
fpm -C ${DIST_ASSEMBLY_DIR} \
    --force \
    --after-remove   ${DIST_ASSEMBLY_DIR}/pkg-bin/after-uninstall.sh \
    --before-install ${DIST_ASSEMBLY_DIR}/pkg-bin/before-install.sh \
    --config-files   /var/awslogs/etc/config/${PACKAGE_NAME}.conf \
    --deb-group ${PACKAGE_NAME} \
    --deb-systemd ${DIST_ASSEMBLY_DIR}/${PACKAGE_NAME}.service \
    --deb-user ${PACKAGE_NAME} \
    --depends "$PACKAGE_DEPENDENCIES" \
    --exclude pkg-bin \
    --exclude *.deb \
    --iteration 1 \
    --name ${PACKAGE_NAME} \
    --package ${DIST_ASSEMBLY_DIR} \
    -s dir \
    -t deb \
    --version ${PACKAGE_VERSION} \
    .


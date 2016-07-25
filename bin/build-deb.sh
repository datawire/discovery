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

BIN_PATH="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
source ${BIN_PATH}/common.sh

PACKAGE_DEPENDENCIES="$(read_prop deb_dependencies)"
PACKAGE_VERSION="$(cat $PACKAGE_PROJECT_MODULE/build/version)"
PACKAGE_RELEASE="$(cat $PACKAGE_PROJECT_MODULE/build/release)"

DIST_ASSEMBLY_DIR=build/distributions/deb

discovery_keystore=${DISCOVERY_KEYSTORE:?Environment variable DISCOVERY_KEYSTORE not set.}
discovery_keystore_password=${DISCOVERY_KEYSTORE_PASSWORD:?Environment variable DISCOVERY_KEYSTORE_PASSWORD not set.}
mixpanel_token=${MIXPANEL_TOKEN:?Environment variable MIXPANEL_TOKEN not set.}

step "Building Debian Image"
APP_ROOT=opt/${ROOT_PROJECT_NAME}
APP_CONFIG_ROOT=${APP_ROOT}/config

# Copy the build output artifact into the assembly directory.

mkdir -p ${DIST_ASSEMBLY_DIR}/${APP_ROOT}
mkdir -p ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}
mkdir -p ${DIST_ASSEMBLY_DIR}/etc/${ROOT_PROJECT_NAME} # legacy stuff
cp ${ROOT_PROJECT_NAME}-web/build/libs/${ROOT_PROJECT_NAME}-web-${PACKAGE_VERSION}-*-fat.jar ${DIST_ASSEMBLY_DIR}/${APP_ROOT}

# The pkg-bin directory is where the various before/after install/uninstall scripts for a Debian package are stored.
mkdir -p ${DIST_ASSEMBLY_DIR}/pkg-bin
cp ${PACKAGE_PROJECT_MODULE}/dist/pkg-bin/* ${DIST_ASSEMBLY_DIR}/pkg-bin
chmod +x ${DIST_ASSEMBLY_DIR}/pkg-bin/*

# Configuration files
mkdir -p ${DIST_ASSEMBLY_DIR}/etc/nginx/sites-available
mkdir -p ${DIST_ASSEMBLY_DIR}/var/awslogs/etc/config

cp ${PACKAGE_PROJECT_MODULE}/config/${ROOT_PROJECT_NAME}-prod.json     ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/
cp ${PACKAGE_PROJECT_MODULE}/config/cluster.xml                        ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/
cp ${PACKAGE_PROJECT_MODULE}/config/cluster-prod.xml                   ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/

# copy the keystore into place
cp ${discovery_keystore}                                               ${DIST_ASSEMBLY_DIR}/etc/${ROOT_PROJECT_NAME}/hmac.jceks

# update the configuration file to reflect where the keystore is and the password it requires
sed -i "s/\${DISCOVERY_KEYSTORE}/\/etc\/${ROOT_PROJECT_NAME}\/hmac.jceks/g" ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/${ROOT_PROJECT_NAME}-prod.json
sed -i "s/\${DISCOVERY_KEYSTORE_PASSWORD}/${DISCOVERY_KEYSTORE_PASSWORD}/g" ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/${ROOT_PROJECT_NAME}-prod.json
sed -i "s/\${MIXPANEL_TOKEN}/${MIXPANEL_TOKEN}/g"                           ${DIST_ASSEMBLY_DIR}/${APP_CONFIG_ROOT}/${ROOT_PROJECT_NAME}-prod.json

cp ${PACKAGE_PROJECT_MODULE}/dist/systemd-${ROOT_PROJECT_NAME}.service ${DIST_ASSEMBLY_DIR}/${PACKAGE_NAME}.service
cp ${PACKAGE_PROJECT_MODULE}/dist/nginx-${ROOT_PROJECT_NAME}.conf      ${DIST_ASSEMBLY_DIR}/etc/nginx/sites-available/${PACKAGE_NAME}.conf
cp ${PACKAGE_PROJECT_MODULE}/dist/cloudwatch-${ROOT_PROJECT_NAME}.conf ${DIST_ASSEMBLY_DIR}/var/awslogs/etc/config/${PACKAGE_NAME}.conf

# Runnable script
mkdir -p ${DIST_ASSEMBLY_DIR}/opt/${PACKAGE_NAME}
cp ${PACKAGE_PROJECT_MODULE}/dist/${ROOT_PROJECT_NAME}.sh ${DIST_ASSEMBLY_DIR}/opt/${PACKAGE_NAME}/${PACKAGE_NAME}.sh

# Run FPM in the distribution assembly directory (-C)
fpm -C ${DIST_ASSEMBLY_DIR} \
    --force \
    --after-install  ${DIST_ASSEMBLY_DIR}/pkg-bin/after-install.sh \
    --after-remove   ${DIST_ASSEMBLY_DIR}/pkg-bin/after-uninstall.sh \
    --before-install ${DIST_ASSEMBLY_DIR}/pkg-bin/before-install.sh \
    --config-files   /var/awslogs/etc/config/${PACKAGE_NAME}.conf \
    --deb-group ${PACKAGE_NAME} \
    --deb-systemd ${DIST_ASSEMBLY_DIR}/${PACKAGE_NAME}.service \
    --deb-user ${PACKAGE_NAME} \
    --depends "$PACKAGE_DEPENDENCIES" \
    --exclude pkg-bin \
    --exclude *.deb \
    --iteration ${PACKAGE_RELEASE} \
    --name ${PACKAGE_NAME} \
    --package ${DIST_ASSEMBLY_DIR} \
    -s dir \
    -t deb \
    --version ${PACKAGE_VERSION} \
    .


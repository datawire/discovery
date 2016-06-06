#!/usr/bin/env bash
set -euo pipefail

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

PROJECT_PROPERTIES_FILE=gradle.properties

read_prop() {
  local prop_name=${1:?Property name not specified}

  local result=$( \
    grep ${prop_name} ${PROJECT_PROPERTIES_FILE} | \
    awk -F= '{print $2}' | \
    sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' \
  )

  if [ -n "$result" ]; then
    printf "$result"
  else
    printf "Property '${prop_name}' not found in '${PROJECT_PROPERTIES_FILE}'."
    exit 1
  fi
}

ROOT_PROJECT_NAME="$(read_prop name)"

# Convert whitespaces and underscores to dashes. The PACKAGE_PROJECT_MODULE for a web service SHOULD always have the
# -web suffix appended to value of ROOT_PROJECT_NAME.
PACKAGE_NAME="$(printf "$ROOT_PROJECT_NAME" | sed -e 's/[ _]/-/g' )"
PACKAGE_PROJECT_MODULE="${PACKAGE_NAME}-web"

# The path where service-specific configuration is stored.
PACKAGE_CONFIG_DIR="$PACKAGE_PROJECT_MODULE/config"

# The path where distribution-specific configuration is stored, for example, nginx, .deb or .rpm specific files.
PACKAGE_DIST_DIR="$PACKAGE_PROJECT_MODULE/dist"

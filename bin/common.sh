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
VERBOSITY=3

# configure colors
if [[ -t 1 ]]; then
    color_support=$(tput colors)
    if [ -n "$color_support" ] && [ ${color_support} -ge 8 ]; then
             bold="$(tput bold)"
        underline="$(tput smul)"
         standout="$(tput smso)"
           normal="$(tput sgr0)"
            black="$(tput setaf 0)"
              red="$(tput setaf 1)"
            green="$(tput setaf 2)"
           yellow="$(tput setaf 3)"
             blue="$(tput setaf 4)"
          magenta="$(tput setaf 5)"
             cyan="$(tput setaf 6)"
            white="$(tput setaf 7)"
    fi
fi

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

sanity_check() {
    header "Build Environment Sanity Checks"
    local required_programs=("$@")
    for p in "${required_programs[@]}"; do
        is_on_path ${p}
    done
}

is_on_path() {
    local program=${1:?Program not specified}
    step "$program installed? "
    if command -v ${program} >/dev/null 2>&1 ; then
        ok
    else
        die
    fi
}

output() {
    local level="$1"
    local format="${2:?Output message format not set}"
    local msg="${3:?Output message not set}"

    if [ ${VERBOSITY} -ge ${level} ]; then
        printf -- "$format" "$msg"
    fi
}

msg() { output 1 "%s\n" "$1"; }
nl()  { printf "\n"; }

header() {
    nl
    msg "*** ${bold}$1${normal} ***"
    nl
}

step()   { output 2 "--> %s" "$1"; }
stepln() { step "$1\n"; }

sub_step () {
    output 3 "-->  %s" "$1"
}

ok() {
    output 3 "%s\n" "${green}OK${normal}"
}

pass() {
    output 3 "%s\n" "${yellow}OK${normal}"
}

die() {
    printf "${red}FAIL${normal}"
    printf "\n\n        "
    printf "${1:?''}"
    printf "\n\n"
    exit 1
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

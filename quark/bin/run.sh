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

DEBUG=0
INTEGRATION="common"
UUID="$(uuidgen | tr [:upper:] [:lower:])"
QUARK_SOURCES="intro.q util.q"

TEMP_DIR=/tmp/test-${UUID}
TEMP_SSH_DIR=${TEMP_DIR}/.ssh

SSH_KEY_NAME="ssh-$UUID"
SSH_PRIVATE_KEY_FILE="$TEMP_SSH_DIR/$SSH_KEY_NAME"
SSH_PUBLIC_KEY_FILE="$SSH_PRIVATE_KEY_FILE.pub"

SSH_OPTS="-i $SSH_PRIVATE_KEY_FILE -o StrictHostKeyChecking=no"
SCP_OPTS=${SSH_OPTS}

# --------------------------------------------------------------------------------------------------
# FUNCTION DEFINITIONS
# --------------------------------------------------------------------------------------------------

out() {
  local level="$1"
  local format="$2"
  local content="$3"
  printf -- "$format" "$content"
}

msgln() {
  out 1 "%s\n" "--> $1"
}

step() {
  out 2 "--> %s\n" "$1"
}

sub_step() {
  out 3 "--> %s" "$1"
}

ok() {
  out 3 "%s\n" "OK"
}

cleanup() {
  if [[ "$INTEGRATION" != "common" ]]; then
    msgln "Cleaning up environment"
    terraform destroy -force -var-file=launch-vars.json
    rm -rf ${TEMP_DIR}
    rm -f introspection.tar.gz
  fi
}

# --------------------------------------------------------------------------------------------------
# MAIN SCRIPT
# --------------------------------------------------------------------------------------------------

while [[ $# > 1 ]]; do
  key="$1"

  case ${key} in
    -i|--integration)
      INTEGRATION="$2"
      shift
      ;;
    *)
      ;;
  esac
  shift
done

case "$INTEGRATION" in
  ec2|common)
    ;;
  *)
    msgln "Unknown integration suite!"
    exit 1
    ;;
esac

msgln "Initializing (run-id: ${UUID})"
mkdir -p ${TEMP_DIR}
mkdir -p ${TEMP_SSH_DIR}

msgln "Generating SSH key pair"
ssh-keygen -q -b 2048 -t rsa -f "$SSH_PRIVATE_KEY_FILE" -N ""
chmod 600 "$SSH_PRIVATE_KEY_FILE"
chmod 600 "$SSH_PUBLIC_KEY_FILE"
chmod 700 ${TEMP_SSH_DIR}

msgln "Assembling Quark Sources"
tar -cvzf introspection.tar.gz ${QUARK_SOURCES}
mv introspection.tar.gz ${TEMP_DIR}/

msgln "Compiling Quark Sources..."
tar -xvzf ${TEMP_DIR}/introspection.tar.gz -C ${TEMP_DIR}/
quark compile --python ${TEMP_DIR}/intro.q

if [[ "${INTEGRATION}" != "common" ]]; then
  msgln "Launching infrastructure"
  cd test/${INTEGRATION}

  cat << EOF > "launch-vars.json"
{
  "ssh_key_name"    : "${SSH_KEY_NAME:?SSH key name not set}",
  "ssh_private_key" : "${SSH_PRIVATE_KEY_FILE:?SSH private key file not set}",
  "ssh_public_key"  : "${SSH_PUBLIC_KEY_FILE:?SSH public key file not set}"
}
EOF

  trap cleanup INT
  trap cleanup EXIT

  chmod +x launch.sh
  ./launch.sh

  msgln "Provisioning infrastructure"
  ./provision.sh "$TEMP_DIR" "$SSH_PRIVATE_KEY_FILE"

  msgln "Running tests"
  ./run.sh "$SSH_PRIVATE_KEY_FILE"
else
  py.test test/common/test_introspection.py
fi
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

TEMP_DIR=${1:?Temporary directory path not set}

SSH_PRIVATE_KEY_FILE=${2:?SSH private key file not set}
SSH_OPTS="-i $SSH_PRIVATE_KEY_FILE -o StrictHostKeyChecking=no"
SSH_REMOTE_USER="$(terraform output ssh_username)"
SSH_REMOTE_HOST="$(terraform output public_ip)"

scp ${SSH_OPTS} ${TEMP_DIR}/introspection.tar.gz "$SSH_REMOTE_USER@$SSH_REMOTE_HOST:/tmp/introspection.tar.gz"
scp ${SSH_OPTS} provision_ec2.sh                 "$SSH_REMOTE_USER@$SSH_REMOTE_HOST:/tmp/provision_ec2.sh"
scp ${SSH_OPTS} test_introspection.py            "$SSH_REMOTE_USER@$SSH_REMOTE_HOST:/tmp/test_introspection.py"

ssh ${SSH_OPTS} "$SSH_REMOTE_USER@$SSH_REMOTE_HOST" 'chmod +x /tmp/provision_ec2.sh; /tmp/provision_ec2.sh'
ssh ${SSH_OPTS} \
    "$SSH_REMOTE_USER@$SSH_REMOTE_HOST" \
    "cd /tmp; source /home/$SSH_REMOTE_USER/.quark/config.sh; quark install --python intro.q"
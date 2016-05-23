#!/usr/bin/env bash
set -euo pipefail
set -x

INTEGRATION="none"
UUID="$(uuidgen | tr [:upper:] [:lower:])"
TEMP_DIR=/tmp/test-${UUID}

TEMP_KEY_NAME="ssh-$UUID"
TEMP_PRIVATE_KEY_FILE="$TEMP_DIR/$TEMP_KEY_NAME"
TEMP_PUBLIC_KEY_FILE="$TEMP_PRIVATE_KEY_FILE.pub"

SSH_OPTS="-i $TEMP_PRIVATE_KEY_FILE -o StrictHostKeyChecking=no"
SCP_OPTS=${SSH_OPTS}

msg() {
  printf "%s\n" "--> ${1:?Message content not set}"
}

setup() {
  msg "Setup test infrastructure..."
  cd test/${INTEGRATION}

  cat << EOF > "terraform.tfvars"
{
  "ssh_key_name": "${1:?SSH key name not set}",
  "ssh_private_key": "${2:?SSH private key file not set}",
  "ssh_public_key": "${3:?SSH public key file not set}"
}
EOF

  terraform apply
  cd -
}

compile_quark() {
  local ssh_username="${1:?Remote host user not set}"
  local ssh_remote_host="${2:?Remote host address not set}"

  msg "Compiling and installing Quark sources..."
  ssh ${SSH_OPTS} "$ssh_username@$ssh_remote_host" "cd /tmp; source /home/$ssh_username/.quark/config.sh; quark install --python intro.q"
}

provision() {
  msg "Provisioning test system..."

  local integration="${1:?Integration test suite not set}"
  local ssh_private_key="${2:?SSH remote private key not set}"
  local ssh_username="${3:?Remote host user not set}"
  local ssh_remote_host="${4:?Remote host address not set}"

  if [ -f test/${integration}/provision.sh ]; then
    #scp -i ${ssh_private_key} -o StrictHostKeyChecking=no test/${integration}/provision.sh "$ssh_username@$ssh_remote_host:/tmp/provision.sh"
    scp ${SCP_OPTS} test/${integration}/provision.sh "$ssh_username@$ssh_remote_host:/tmp/provision.sh"
  fi

  tar -cvzf intro.tar.gz intro.q util.q
  scp ${SCP_OPTS} intro.tar.gz "$ssh_username@$ssh_remote_host:/tmp/intro.tar.gz"

  ssh ${SSH_OPTS} ${ssh_username}@${ssh_remote_host} '/tmp/provision.sh'

  compile_quark "$ssh_username" "$ssh_remote_host"
}

cleanup() {
  msg "Cleaning up environment..."

  cd test/${INTEGRATION}
  terraform destroy -force
  cd -

  rm -rf ${TEMP_DIR}
  rm -f intro.tar.gz
}

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

trap cleanup INT
trap cleanup EXIT

case "$INTEGRATION" in
  ec2)
    ;;
  *)
    msg "Unknown integration suite!"
    exit 1
    ;;
esac

msg "Integration Suite = $INTEGRATION"
mkdir -p ${TEMP_DIR}

msg "Generate a temporary SSH key pair"

ssh-keygen -q -b 2048 -t rsa -f "$TEMP_PRIVATE_KEY_FILE" -N ""
chmod 400 "$TEMP_PRIVATE_KEY_FILE"
chmod 400 "$TEMP_PRIVATE_KEY_FILE.pub"

setup "$TEMP_KEY_NAME" "$TEMP_PRIVATE_KEY_FILE" "$TEMP_PUBLIC_KEY_FILE"

SSH_USERNAME="$(terraform output -state=test/${INTEGRATION}/terraform.tfstate ssh_username)"
SSH_REMOTE_HOST="$(terraform output -state=test/${INTEGRATION}/terraform.tfstate public_ip)"

provision ${INTEGRATION} ${TEMP_PRIVATE_KEY_FILE} ${SSH_USERNAME} ${SSH_REMOTE_HOST}

msg "Run tests..."
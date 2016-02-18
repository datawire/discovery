#!/bin/bash
set -e
set -u
set -o pipefail

# --------------------------------------------------------------------------------------------------
# Bootstraps a Travis CI environment with the necessary tools to build and test the project.
#

build_dir=build
bin_dir=build/bin

packer_architecture=linux_amd64
packer_version=0.8.6
packer_archive=packer_${packer_version}_${packer_architecture}.zip
packer_url=https://releases.hashicorp.com/packer/${packer_version}/${packer_archive}

# Packer.io
#

mkdir -p ${bin_dir}
if [ ! -f ${packer_archive} ]; then
  wget -O "${packer_archive}" "${packer_url}"
fi

unzip -o -d ${bin_dir} ${packer_archive}

# Datawire Quark
#
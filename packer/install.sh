#!/bin/bash -ex

install_root="$(printenv INSTALL_ROOT)"
package_base_name="$(printenv PACKAGE_BASE_NAME)"
package_version="$(printenv PACKAGE_VERSION)"
package_name="${package_base_name}-${package_version}"

if [ -s "/tmp/packages.lst" ]; then
    echo "--> installing dependencies"
    dnf -y install $(cat /tmp/packages.lst)
    rm "/tmp/packages.lst"
fi

echo "--> extracting onto the filesystem (package: ${package_name} to: ${install_root})"
mkdir -p ${install_root}
mv /tmp/${package_name}.zip ${install_root}/${package_name}.zip
unzip ${install_root}/${package_name}.zip -d ${install_root}
rm ${install_root}/${package_name}.zip
ln -s ${install_root}/${package_name} ${install_root}/${package_base_name}
chmod +x ${install_root}/${package_base_name}/bin/${package_base_name}

echo "--> installing systemd unit file and configuring to start on boot"
mv ${install_root}/${package_base_name}/${package_base_name}.service  /lib/systemd/system/${package_base_name}.service
systemctl enable ${package_base_name}